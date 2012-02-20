(ns
  ^{:in-api-doc true
    :api-doc-title "User management"
    :api-doc "Functions to retrieve and modify user data."}
  leiningen.views.user)

(defmacro defpage [& args]
  nil
  )

(defpage
  ^{:api-doc "Shows a list of friends for a user."
    :api-url "http://api.playmakerstudios.com/1/lineup/user/<id>/friends/show.json?accessToken=<accessToken>"
    :api-login-required true
    :api-parameters {'id {:required true :doc "The ID of the user known to the device or \"user\" for the current user"}
                     'accessToken {:required true :doc "The access token to access the protected API."}}
    :api-request-header { "If-Modified-Since" {:required false :doc "The function may return not modified (304) if the friend list has not changed since that date."}
                           "If-None-Match" {:required false :doc "The function may return not modified (304) if the identifier is the latest update to the friend list."}}
    :api-response { 'id {:required true :doc "The ID of the relationship."}
                    'type {:required true :doc "The type of the relationship (e.g. friend, proposal, etc.)"}
                    'facebookId {:required false :doc "The ID of the facebook entity."}
                    'firstName {:required true :doc "The first name of the friend"}
                    'lastName {:required true :doc "The last name of the friend"}
                    'source {:required true :doc "The initial source of the relationship (e.g. facebook)"}}
    :api-response-header { "Last-Modified" {:required false :doc "The date of the last modification of the friend list."}
                           "ETag" {:required false :doc "A strong identifier of the last change of the friend list."}}
    :api-http-status { 304 "The friend list has not changed." }
    :api-example {:request "curl 'http://api.playmakerstudios.com/1/account/user/friends/show.json?accessToken=AAAAAOE9...yJTZRlI-BnO1ds'"
                  :response "[{
    \"type\": \"friend\",
    \"id\": \"4ef48dcd30042141fef9fd7e\",
    \"facebookId\": \"603663178\",
    \"firstName\": \"Max\",
    \"lastName\": \"Mustermann\",
    \"source\": \"facebook\"
}]"}}
  show-friends
  [:get ["/:version/account/:id/friends/show.json" :version #"\d+" :id #"[A-Za-z0-9]*"]] {:keys [version id skip limit]}
  "A REST-API to return the user information based on the device ID of one of the users devices."

  )


(defpage
  ^{:api-doc-title "List friendship proposals"
    :api-doc "Makes a list of proposed friends for a user."
    :api-url "http://api.playmakerstudios.com/1/lineup/user/<id>/friends/propose.json?accessToken=<accessToken>"
    :api-login-required true
    :api-parameters {'id {:required true :doc "The ID of the user known to the device or \"user\" for the current user"}
                     'accessToken {:required true :doc "The access token to access the protected API."}}
    :api-example {:request "curl 'http://api.playmakerstudios.com/1/account/user/propose/show.json?accessToken=AAAAAOE9JTcAAAFDd13cMnoDP2jMvP9NY8FsA1rI2jdydWioSVqu8vlEh8Pa8OVp1gZ22LJHC2j7AE_Fofo2__dZmMeo4yJTZRlI-BnO1ds'"
                  :response "[{
    \"type\": \"proposal\",
    \"id\": \"4ef373e13004c374fd2926e7\",
    \"facebookId\": \"...\",
    \"firstName\": \"...\",
    \"lastName\": \"...\",
    \"source\": \"facebook\"
},
{
    \"type\": \"proposal\",
    \"id\": \"4ef373e13004c374fd2926e8\",
    \"facebookId\": \"...\",
    \"firstName\": \"...\",
    \"lastName\": \"...\",
    \"source\": \"facebook\"
},
{
    \"type\": \"proposal\",
    \"id\": \"4ef373e13004c374fd2926eb\",
    \"facebookId\": \"...\",
    \"firstName\": \"...\",
    \"lastName\": \"...\",
    \"source\": \"facebook\"
}]"}}
  propose-friends
  [:get ["/:version/account/:id/friends/propose.json" :version #"\d+" :id #"[A-Za-z0-9]*"]] {:keys [version id skip limit]}
  "A REST-API to return the user information based on the device ID of one of the users devices."

  )