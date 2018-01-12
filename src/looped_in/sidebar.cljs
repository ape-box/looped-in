(ns looped-in.sidebar
  (:require [goog.dom :as dom]
            [goog.html.sanitizer.HtmlSanitizer :as Sanitizer]
            [cljs.core.async :refer [go <!]]
            [looped-in.hackernews :as hn]
            [looped-in.promises :refer [promise->channel]])
  (:import (goog.ui Zippy)))

(defn log [& args]
  (let [bg (-> js/browser (.-extension) (.getBackgroundPage))]
    (apply (-> bg (.-console) (.-log)) "[Looped In]" (map clj->js args))))

(defn comment-dom [{:strs [text author children]}]
  (let [$text (dom/createDom "div"
                             #js {:class "commentText"}
                             (dom/safeHtmlToNode (Sanitizer/sanitize text)))
        $author (dom/createDom "div"
                              #js {:class "commentAuthor"}
                              author)]
    (if (> (count children) 0)
      (let [$toggle (dom/createDom "div"
                                   #js {:class "commentToggle"}
                                   "<Toggle children>")
            $children (apply dom/createDom
                             "div"
                             #js {:class "commentChildren"}
                             (clj->js (map comment-dom children)))]
        (Zippy. $toggle $children)
        (dom/createDom "div"
                       #js {:class "comment"}
                       $text
                       $author
                       $toggle
                       $children))
      (dom/createDom "div"
                     #js {:class "comment"}
                     $text
                     $author))))

(defn comments-dom [comments]
  (clj->js
   (apply dom/createDom
          "div"
          #js {:class "comments"}
          (map comment-dom comments))))

(defn story-dom [story]
  (let [$title (dom/createDom "div"
                              #js {:class "storyTitle"}
                              (story "title"))
        $comments (comments-dom (filter #(= "comment" (% "type")) (story "children")))]
    (Zippy. $title $comments)
    (dom/createDom "div"
                   #js {:class "story"}
                   $title
                   $comments)))

(defn render-items [items]
  (let [stories (filter #(= "story" (% "type")) items)
        $stories (clj->js (map story-dom stories))
        $storiesContainer (dom/getElement "storiesContainer")]
    (log items)
    (dom/append $storiesContainer $stories)))

(go (-> js/browser
        (.-runtime)
        (.sendMessage #js {:type "popupOpened"})
        (promise->channel)
        (<!)
        (hn/fetch-items)
        (<!)
        ((fn [items] (filter #(not (nil? %)) items)))
        (render-items)))
