(ns umeng.tooling.nbb.supabase-access-token
  (:require ["@supabase/supabase-js" :as supa]
            [promesa.core :as p]))

(p/let [[supa-url
         supa-anon-key ;; get by running supabase status
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

  #_(when signup
    (-> maybe-new-user
        (. -error)
        (println)))

  #_(println "----------------- access token")

  (prn {:access-token (-> maybe-login
                          (. -data)
                          (. -session)
                          (. -access_token))}))
