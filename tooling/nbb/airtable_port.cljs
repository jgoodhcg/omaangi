(ns umeng.tooling.nbb.airtable-port
  (:require ["@supabase/supabase-js" :as supa]))

(def supa-url (first *command-line-args*))
(def supa-anon-key (second *command-line-args*))
(def supa-client (supa/createClient supa-url supa-anon-key))
