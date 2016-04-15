# semi-supervised-event-extraction
Code for 2015 summer nlp reseach project at Carnegie Mellon University

### external jar libraries

The project uses [jsoup](http://jsoup.org) to parse html to documents and [stanford-corenlp](http://stanfordnlp.github.io/CoreNLP/) to build semantic graph dependencies from a sentence.

###Overview

This takes input a directory to collections of documents and return a csv file which contains all event mentions of specific verbs and types that are in the documents.

###Procedure

First, it parses HTML file to text using Jsoup and searches through the documents for sentences which contain verbs of interest. Next, using Stanford CoreNLP, it generates semantic sentence-bound graphs. Next it traverses through the graph with a set of rules to retrieve event's actor, actee and related arguments in a single dataset.

The dataset is then converted to binary features and ran with EM (using a small number of training data). The output class is the class type of an argument (time, date, location, etc). 

###Implementation

EM is implemented using [link](http://www.cs.columbia.edu/~mcollins/em.pdf). 
