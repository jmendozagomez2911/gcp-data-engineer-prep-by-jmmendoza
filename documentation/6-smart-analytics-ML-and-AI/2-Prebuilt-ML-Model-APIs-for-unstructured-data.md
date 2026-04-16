# 🧠 Prebuilt ML Model APIs for Unstructured Data

## Exam-focused summary

## Core idea

This module is about using **Google Cloud’s ready-to-use ML APIs** on **unstructured data**.

### Unstructured data includes:

* text
* images
* audio
* video

### Main message

Unstructured data is valuable, but hard to process manually.
Google Cloud provides **pretrained APIs** so you can extract meaning from it **without training your own model**.

---

## 1) Why unstructured data is difficult

Unlike structured data, unstructured data does not come in neat rows and columns.

Examples:

* e-mails
* comments
* articles
* photos
* medical images
* voice or video

From an image or a paragraph of text, businesses may want to know things like:

* what language is this?
* what topic is this about?
* what sentiment does it express?
* which objects, people, or places appear in it?

### Key point

The hard part is not just storing unstructured data.
The hard part is **extracting metadata and meaning from it**.

---

## 2) Why pretrained APIs matter

Training ML models on unstructured data is hard because it usually requires:

* lots of labelled data
* significant compute
* specialised ML skills

That is out of reach for many organisations.

### What Google Cloud offers

Google provides **pretrained APIs** such as:

* **Vision API**
* **Dialogflow**
* **Cloud Natural Language API**

These use **Google’s pretrained models**, and you send **your data** to them for prediction.

### Key exam point

> Pretrained APIs let you apply ML to your data **without training your own model**.

---

## 3) Main limitation of pretrained APIs

This is an important caveat.

Pretrained APIs work well **only if your data is within the scope of what the model was trained for**.

If your use case is too specialised, the results may be poor.

### Exam takeaway

> Pretrained APIs are fast and easy, but they may not perform well on very domain-specific data.

---

## 4) Example business use cases mentioned

### **Dialogflow / Uniqlo**

Used to build a shopping chatbot.

Use case:

* conversational user interfaces
* web apps
* mobile apps
* bots
* IVR systems

### **Vision-type image use cases**

Examples included:

* distinguishing clouds from snow in satellite imagery
* tracking ships from satellite images
* medical image analysis
* detecting product defects

### Why these examples matter

They show that ML on unstructured data is useful for:

* automation
* classification
* search
* customer interaction
* quality control
* forecasting

---

## 5) Structured vs unstructured data

### **Structured data**

Usually comes from:

* relational databases
* ERP systems
* spreadsheets
* inventory systems

It follows strict schema and formatting rules.

### **Unstructured data**

Usually comes from:

* text
* e-mail
* images
* audio
* video
* social comments

It does not follow a strict tabular structure.

### Important point from the module

A very large share of business data is unstructured.

---

## 6) Cloud Natural Language API

This module focuses mostly on the **Cloud Natural Language API** for text.

The main idea is to **enrich text** by applying labels or extracting meaning.

Examples:

* What is this e-mail about?
* Is this review positive or negative?
* Which people or organisations are mentioned?

---

## 7) Main features of the Natural Language API

### **1. Syntactic analysis**

Breaks text into tokens and sentences and adds grammatical information.

Examples of what it can identify:

* noun / verb
* singular / plural
* tense
* mood
* voice
* grammatical role

### **2. Entity analysis**

Identifies items such as:

* people
* places
* organisations
* events
* products
* dates
* phone numbers
* addresses
* numbers

### **3. Sentiment analysis**

Measures overall attitude in the text.

It returns:

* a **score**
* a **magnitude**

Important detail:

* it does **not** identify precise emotions like “anger” vs “sadness”
* instead it gives a more general positive / negative / neutral interpretation

### **4. Content classification**

Classifies text into topic categories.

The transcript mentions classification into hundreds of categories.

### Key exam point

> Natural Language API can do **syntax, entities, sentiment, and content classification**.

---

## 8) Sentiment analysis detail that is easy to forget

Sentiment is not just yes/no.

It is represented numerically:

* **score** → direction of sentiment
* **magnitude** → strength of sentiment

Also, the module explicitly says you should define your own thresholds depending on your use case.

### Why that matters

A small sentiment value may not be meaningful enough for business action.

---

## 9) Lab flow: what you actually do

The lab is mainly about **text classification**.

### Workflow

1. Enable the **Cloud Natural Language API**
2. Create an **API key**
3. Send text to `classifyText`
4. Read many `.txt` files from **Cloud Storage**
5. Classify them with the Natural Language API
6. Store results in **BigQuery**
7. Query and analyse the categories in BigQuery

### Why this matters for the exam

It shows a common pattern:

> **GCS → ML API → BigQuery**

That is a very realistic Google Cloud design pattern for analytics.

---

## 10) Classification output

The API returns categories plus a confidence score.

Example output structure:

* `category`
* `confidence`

In the lab, only the **first returned category** is stored in BigQuery to keep things simple.

---

## 11) BigQuery’s role here

BigQuery is used to:

* store classified text results
* query category frequencies
* filter by confidence
* analyse large text corpora after enrichment

### Important idea

ML APIs often generate metadata, and **BigQuery is then used to analyse that enriched data at scale**.

---

# 🔥 What really matters for the exam

## Most important points

* Unstructured data includes **text, images, audio, and video**
* It is hard to analyse because it lacks strict structure
* Google Cloud provides **pretrained ML APIs** for unstructured data
* These APIs use **Google’s pretrained models**
* You send **your data** to the API for predictions
* Main downside: results may be poor if your data is outside the model’s intended scope
* The **Cloud Natural Language API** supports:

    * syntactic analysis
    * entity analysis
    * sentiment analysis
    * content classification
* Sentiment returns **score** and **magnitude**
* BigQuery is a common place to store and analyse enriched results after API inference

---

# 🧪 Interview version

If they ask how Google Cloud helps with unstructured data, a strong answer is:

> Google Cloud provides pretrained APIs such as Vision API, Dialogflow, and the Natural Language API. These let you extract meaning from unstructured data without training your own models. You send your data to Google’s pretrained models, receive predictions or labels back, and then store or analyse those results in systems like BigQuery.

If they ask for the main trade-off of pretrained APIs, say:

> The main advantage is speed and simplicity, but the limitation is that they work best only when your data is similar to the data and tasks the pretrained model was designed for.

---

# Quiz answers

## 1) Google Cloud's pretrained model APIs use:

**Google’s models and your data**

## 2) True or False? Most business data is unstructured data, and mainly text.

**False**

Why false:

* the module says much business data is unstructured
* but unstructured data is **not mainly text only**
* it also includes **audio, video, images, e-mail, comments, and more**

---

# One-line takeaway

> **Pretrained ML APIs let you extract value from unstructured data quickly, but only when your data fits the scope of the pretrained model.**
