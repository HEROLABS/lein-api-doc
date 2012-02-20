;   Copyright (c) 2012 Playmaker Studio GmbH. All rights reserved.
;
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
;
;   Unless required by applicable law or agreed to in writing, software
;   distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
;   WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
;   License for the specific language governing permissions and limitations under
;   the License.

(ns leiningen.api-doc
  (:require [leiningen.compile :as compile])
  (:use [clojure.tools.namespace :only [find-namespaces-in-dir find-clojure-sources-in-dir read-file-ns-decl]]
        [clojure.string :only [split lower-case trim upper-case]]
        [clojure.java.io :only [file make-parents]]
        [leiningen.util.paths :only (ns->path)]
        )
  (:import [java.io File FileReader BufferedReader PushbackReader
            InputStreamReader]
           [java.text DateFormat])
  )

(def date-format (DateFormat/getDateTimeInstance DateFormat/SHORT DateFormat/SHORT java.util.Locale/GERMANY))

;; Pulled from old-contrib to avoid dependency
(defn as-str
  ([] "")
  ([x] (if (instance? clojure.lang.Named x)
         (name x)
         (str x)))
  ([x & ys]
    ((fn [^StringBuilder sb more]
       (if more
         (recur (. sb (append (as-str (first more)))) (next more))
         (str sb)))
      (new StringBuilder ^String (as-str x)) ys)))

(defn escape-html
  "Change special characters into HTML character entities."
  [text]
  (.. ^String (as-str text)
    (replace "&" "&amp;")
    (replace "<" "&lt;")
    (replace ">" "&gt;")
    (replace "\"" "&quot;")))

(defn stringify
  "Flat string represenation."
  {:tag String
   :static true}
  (^String [] "")
  (^String [^Object x]
    (if (or (coll? x) (nil? x)) (apply stringify x) (. x (toString))))
  (^String [x & ys]
    ((fn [^StringBuilder sb more]
       (if more
         (recur (. sb (append (stringify (first more)))) (next more))
         (str sb)))
      (new StringBuilder (str x)) ys)))

(defn ns-decl?
  "Returns true if form is a (ns ...) declaration."
  [form]
  (and (list? form) (= 'ns (first form))))

(defn defpage-decl?
  "Returns true if form is a (defpage ...) declaration."
  [form]
  (and (list? form) (= 'defpage (first form))))

(defn- to-path [ns]
  (.replace (as-str ns) "." (str java.io.File/separatorChar))
  )

(defn- source-dir [project dir]
  "Converts a specified directory into a file directory"
  (file (if (:api-doc-test project) (:test-path project) (:source-path project)) (to-path dir))
  )

(defn source-files [project]
  "Builds a list of clojure source files to scan for a name space to be documented."
  (reduce (fn [srcs dir] (concat srcs (find-clojure-sources-in-dir (source-dir project dir)))) (list) (:api-doc project))
  )

(defn find-view-files [project]
  "Searches all files with namespaces to document for a project."
  (for [source (source-files project)
        :let [decl (read-file-ns-decl source)]
        :when (:in-api-doc (meta (second decl)))
        ]
    source
    )
  )

(defn- extract-defpage [form]
  "Extracts some informations from noir defpage macro"
  (let [data (meta (second form))
        symbol (with-meta (second form) nil)
        params (second (rest form))
        http-method (if (vector? params) (first params) nil)]
    (if http-method [(assoc data :http-method http-method) symbol] [data symbol])
    )
  )

(defn- read-doc [file]
  "Reads the clojure forms from a .clj file."
  (with-open [reader (PushbackReader. (BufferedReader. (FileReader. file)))]
    (loop [rdr reader doc {}]
      (let [form (read rdr false nil true)]
        (cond
          (nil? form) doc
          (ns-decl? form) (recur rdr (assoc doc :ns (merge {:nsname (name (second form))} (meta (second form)))))
          (defpage-decl? form) (let [[data symbol] (extract-defpage form)]
                                 (recur rdr (update-in doc [:api ] assoc symbol data)))
          :else (recur rdr doc)
          )
        )
      ))
  )

(defn- ns-doc-title [view]
  "Creates a printable doc title"
  (if-let [api-doc-meta (:api-doc-title (:ns view))]
    api-doc-meta
    (:nsname (:ns view))))


(defn- api-endpoint
  "Creates a printable API endpoint description"
  ([data] (api-endpoint data true))
  ([data escape]
    (stringify (upper-case (as-str (:http-method data))) " "
      (if escape (escape-html (:api-url data)) (:api-url data)))
    )

  )

(defn api-endpoint-toc-line [name meta-data]
  "Creates a printable table of content line for an API endpoint"
  (stringify \"
    (if (not (empty? (:api-doc-title meta-data)))
      (:api-doc-title meta-data)
      (api-endpoint meta-data))
    \" ":#" name
    ))



(defn textile-file-name [root view]
  "Creates a textile file name for a root"
  (let [name (lower-case (as-str (:nsname (:ns view))))
        dir-path (rest (reverse (split name #"\.")))
        fname (str (.replace name "." "_") ".textile")
        path (apply str (interpose java.io.File/separatorChar (reverse (cons fname dir-path))))
        ]
    (file root path)
    )
  )

(defn- enrich-http-status-description [descr]
  "Addes some default documentation to HTTP status codes"
  (sort (merge {
                 200 "Request was successful."
                 ; 304 "The entity requested did not change. If any operation expected was not executed."
                 400 "The request contained errors (e.g. some fields contained incorrect values)."
                 401 "The user was not allowed to perform the operation."
                 404 "The request referred to entities that could not be found."
                 } descr))
  )

(defn- sign []
  "Signes a wiki page"
  (stringify "\n***\n\n??Created at " (.format date-format (java.util.Date.)) " by " (System/getProperty "user.name") "??")
  )

(defn- create-textile-page [root view]
  "Creates a textile wiki page"
  (let [out (textile-file-name root view)
        {:keys [ns api]} view]
    (println (str "Writing file '" out "'."))
    (make-parents out)
    (spit out
      (stringify
        "h1. " (ns-doc-title view) "\n\n"
        (when (string? (:api-doc ns)) [(:api-doc ns) "\n\n"])
        "h4. Functions\n\n"
        (for [func api :let [name (key func) meta (val func)]]
          (stringify "* \""
            (if (not (empty? (:api-doc-title meta)))
              (:api-doc-title meta)
              (api-endpoint meta)) \" ":#" name
            "\n")
          ) "\n\n***\n\n"

        (for [func api :let [name (key func) meta (val func)]]
          (stringify
            "\nh2. <a name=\"wiki-" name "\"></a>" (:api-doc-title meta name) "\n"
            "```text\n" (api-endpoint meta false) "\n```" "\n\n"
            (:api-doc meta) "\n\n\n"
            (when (or (:api-parameters meta) (:api-request-header meta) (:request (:api-example meta)))
              ["h3. Request\n\n"
               (when (:api-parameters meta)
                 ["h4. Parameters\n\n"
                  "|_.Name |_.Required |_.Description |\n"
                  (for [[parameterName {:keys [required doc]}] (:api-parameters meta)]
                    (str "|" (eval parameterName) " | " (if required "yes" "no") " | " doc "|\n")) "\n\n"
                  ])
               (when (:api-request-header meta)
                 ["h4. HTTP Header\n\n"
                  "|_.Name |_.Required |_.Description |\n"
                  (for [[parameterName {:keys [required doc]}] (:api-request-header meta)]
                    (str "|" (eval parameterName) " | " (if required "yes" "no") " | " doc "|\n")) "\n\n"
                  ])
               (when-let [request (:request (:api-example meta))]
                 (when (not (empty? (trim request)))
                   (stringify "h4. Example\n\n"
                     "```text\n" request "\n" "```" "\n\n")))
               ])
            (when (or (:api-response meta) (:api-http-status meta) (:api-response-header meta) (:response (:api-example meta)))
              ["h3. Response\n\n"
               (when (:api-response meta)
                 ["h4. Fields\n\n"
                  "|_.Name |_.Required |_.Description |\n"
                  (for [[parameterName {:keys [required doc]}] (:api-response meta)]
                    (str "|" (eval parameterName) " | " (if required "yes" "no") " | " doc "|\n")) "\n\n"
                  ])
               (when (:api-http-status meta)
                 ["h4. HTTP Status codes\n\n"
                  "|_.Code |_.Description |\n"
                  (for [[code doc] (enrich-http-status-description (:api-http-status meta))]
                    (str "|" code " | " doc "|\n")) "\n\n"
                  ])
               (when (:api-response-header meta)
                 ["h4. HTTP Header\n\n"
                  "|_.Name |_.Required |_.Description |\n"
                  (for [[parameterName {:keys [required doc]}] (:api-response-header meta)]
                    (str "|" (eval parameterName) " | " (if required "yes" "no") " | " doc "|\n")) "\n\n"
                  ])
               (when-let [response (:response (:api-example meta))]
                 (when (not (empty? (trim response)))
                   (stringify "h4. Example\n\n"
                     "```text\n" response "\n" "```" "\n\n")))
               ])
            "\n\n***\n\n")
          )
        (sign))
      )
    )
  )

(defn wiki-link [view name]
  "Creates a wiki link"
  (str (.replace (as-str (:nsname (:ns view))) "." "_") (when name (str "#wiki-" name)))
  )

(defn create-index-page [root views]
  "Creates an API doc index page for the wiki"
  (let [out (file root "API-documentation.textile")]
    (println "Writing index file:" out)
    (spit out
      (stringify
        "h1. API documentation\n\n"
        "h2. Table of contents\n\n"
        (for [view views :let [{:keys [ns api]} view]]
          (stringify
            "h3. " (ns-doc-title view) "\n\n"
            (for [func api :let [name (key func) meta (val func)]]
              (stringify "* \""
                (if (not (empty? (:api-doc-title meta)))
                  (:api-doc-title meta)
                  (api-endpoint meta)) \" ":" (wiki-link view name)
                "\n")
              ) "\n\n"

            )
          ) (sign)
        )
      )
    )
  )

(defn create-sidebar-page [root views]
  "Creates a sidebar page"
  (let [out (file root "../_Sidebar.textile")]
    (println "Writing sidebar file:" out)
    (spit out
      (stringify
        "h3. API documentation\n\n"
        (for [view views :let [{:keys [ns api]} view]]
          (stringify
            "* \"" (ns-doc-title view) "\":" (wiki-link view nil) "\n"
            )
          )
        )
      )
    )
  )



(defn api-doc [project & args]
  "Leiningen call back function to generate the API documentation"
  (let [root (file (first args))
        sources (sort (find-view-files project))
        views (for [view sources] (read-doc view))]
    (make-parents root)
    (doseq [view views] (create-textile-page (first args) view))
    (create-index-page root views)
    (create-sidebar-page root views)
    )
  )


