(ns
  ^{:in-api-doc true
    :api-doc-title "Device authentication"
    :api-doc "Functions to authenticate a device or user to use the API."}
  leiningen.views.auth)


(defmacro defpage [& args]
  nil
  )


(defpage
  ^{:api-doc-title "Create an access token for a device"
    :api-doc "Retrieves an access token for a user."
    :api-url "http://api.playmakerstudios.com/1/auth/token.json?userId=<userId>&secret=<secret>"
    :api-login-required false
    :api-parameters {'userId {:required true :doc "The ID of the user known to the device."}
                     'secret {:required true :doc "Secret passphrase for the device optained during user creation."}}
    :api-example {:request "curl 'http://api.playmakerstudios.com/1/auth/token.json' -d '{\"userId\": \"4ee9e7d53004d4352ff272db\", \"secret\": \"AAA...DrJ-f-2vQ\"}'"
                  :response "{ \"accessToken\":\"AAAAAOE9JTcAAAFDd13cMnoDP2jMvP9NY8FsA1rI2jdydWioSVqu8vlEh8Pa8OVp1gZ22LJHC2j7AE_Fofo2__dZmMeo4yJTZRlI-BnO1ds\" }"}}
  create-auth-token [:post ["/:version/auth/token.json" :version #"\d+"]] {:keys [version]}
  "A REST-API to return the user information based on the user ID."

  )