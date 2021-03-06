import json
import datetime

import webapp2

from ml.data import trials
from ml.trial import Trial
from ml.svm import model

clf = model(trials)


class Prediction(webapp2.RequestHandler):
    def post(self):
        print "/prediction"

        data = json.loads(self.request.body)
        t = Trial(data)

        res = clf.predcit(t.X())

        if res < 0:
          res = 0.0

        self.response.headers.add_header("Access-Control-Allow-Origin", "*")
        self.response.headers.add_header("Access-Control-Allow-Methods", "GET, POST")
        self.response.headers.add_header("Access-Control-Allow-Headers", "origin, x-requested-with, content-type, accept")
        self.response.headers.add_header('Content-Type', 'text/plain')
        self.response.write(str(res))


class Root(webapp2.RequestHandler):

    def get(self):
        print "/"

        self.response.write("hej")


app = webapp2.WSGIApplication([
    ("/prediction", Prediction),
    ("/", Root),
])


def main():
    from paste import httpserver
    httpserver.serve(app, host='127.0.0.1', port='8080')

if __name__ == '__main__':
    main()