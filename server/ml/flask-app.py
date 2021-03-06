import json
import datetime

from flask import Flask

from ml.data import trials
from ml.trial import Trial
from ml.svm import model

clf = model(trials)


app = Flask(__name__)

@app.route('/prediction', methods = ['POST'])
def prediction():
    print "/prediction"

    data = json.loads(request.body)
    t = Trial(data)

    res = clf.predcit(t.X())

    if res < 0:
      res = 0.0

    return str(res)





@app.route('/')
def give_greeting():
    print "hello"
    
    return 'hello'


if __name__ == "__main__":
    # Setting debug to True enables debug output. This line should be
    # removed before deploying a production app.
    app.debug = True
    app.run()