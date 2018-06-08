import json
import datetime

from google.appengine.ext import db
from google.appengine.api import datastore_types

import webapp2

import zipfile
import StringIO

import zlib
import marshal


from ml.data import trials
from ml.trial import Trial


# COMPRESSED JSON CLASS
MARSHAL_VERSION = 2
COMPRESSION_LEVEL = 1

class JsonMarshalZipProperty(db.BlobProperty):
  """Stores a JSON serializable object using zlib and marshal in a db.Blob"""

  def default_value(self):
    return None
  
  def get_value_for_datastore(self, model_instance):
    value = self.__get__(model_instance, model_instance.__class__)
    if value is None:
      return None
    return db.Blob(zlib.compress(marshal.dumps(value, MARSHAL_VERSION),
                                 COMPRESSION_LEVEL))

  def make_value_from_datastore(self, value):
    if value is not None:
      return marshal.loads(zlib.decompress(value))
    return value

  data_type = datastore_types.Blob
  
  def validate(self, value):
    return value


class EntryModel(db.Model):
    date = db.DateTimeProperty(auto_now_add=True)
    log = JsonMarshalZipProperty()


class Entry(webapp2.RequestHandler):
    
    def post(self):

        em = EntryModel()

        try:
            data = json.loads(self.request.body)
        except Exception as e:
            res = { "status": "error", "msg": str(e) }
            self.response.write(json.dumps(res) + '\n')
            return

        try:
            em.log = data
            em.put()
        except Exception as e:
            res = { "status": "error", "msg": str(e) }
            self.response.write(json.dumps(res) + '\n')
            return

        res = { "status": "ok" }

        self.response.headers.add_header("Access-Control-Allow-Origin", "*")
        self.response.headers.add_header("Access-Control-Allow-Methods", "GET, POST")
        self.response.headers.add_header("Access-Control-Allow-Headers", "origin, x-requested-with, content-type, accept")
        self.response.headers.add_header('Content-Type', 'text/plain')

        self.response.write(json.dumps(res) + '\n')

class O():
  pass


class Data(webapp2.RequestHandler):
    def get(self):

        output = StringIO.StringIO()

        data = EntryModel.all()

        trial = O()
        trial.data = [d.log for d in data]

        for chunk in json.JSONEncoder().iterencode(trial.__dict__):
            output.write(chunk)


        filename = "data.json"

        output.seek(0)
        self.response.headers['Content-Type'] ='application/text'
        self.response.headers['Content-Disposition'] = 'attachment; filename="'+filename+'"'
        self.response.out.write(output.getvalue())


from ml.svm import model
from ml.data import trials

class Prediction(webapp2.RequestHandler):
    def post(self):
        data = json.loads(self.request.body)
        t = Trial(data)

        clf = model(trials)
        res = clf.predcit(t.X())

        if res < 0:
          res = 0.0

        self.response.headers.add_header("Access-Control-Allow-Origin", "*")
        self.response.headers.add_header("Access-Control-Allow-Methods", "GET, POST")
        self.response.headers.add_header("Access-Control-Allow-Headers", "origin, x-requested-with, content-type, accept")
        self.response.headers.add_header('Content-Type', 'text/plain')
        self.response.write(str(res))



app = webapp2.WSGIApplication([
    ("/entry", Entry),
    ("/data", Data),
    ("/prediction", Prediction)
])