(ns umeng.tooling.nbb.supabase-access-token
  (:require ["@supabase/supabase-js" :as supa]
            [promesa.core :as p]))

(p/let [[supa-url
         supa-anon-key
         email
         password
         signup] *command-line-args*

        supa-client    (supa/createClient supa-url supa-anon-key)
        maybe-new-user (when signup (p/do! (. (. supa-client -auth) signUp #js {:email email :password password})))
        maybe-login    (p/do! (. (. supa-client -auth) signInWithPassword #js {:email email :password password}))
      ]

  #_(println {:supa-url      supa-url
              :supa-anon-key supa-anon-key
              :email         email
              :password      password
              :signup        signup})

  (when signup
    (-> maybe-new-user
        (. -error)
        (println)))

  (println "----------------- access token")
  (-> maybe-login
        (. -data)
        (. -session)
        (. -access_token)
        (println)))
