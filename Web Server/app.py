# import the needed packets
from flask import Flask, request, abort, render_template, jsonify
from flask.ext.bootstrap import Bootstrap

import string, sys
import urllib
import pandas as pd
import numpy as np

app = Flask(__name__)
Bootstrap(app)

@app.route('/')
def index():
	data = refresh()
	return render_template('index.html', data = data)

def refresh():
	conn = urllib.urlopen("http://52.6.200.164/get2.php")
	msg = conn.read()
	info = msg.split("\n")
	jsonarray = info[0]
	d = pd.read_json(jsonarray)
	axis = np.array(d[['name', 'value', 'google']].values)
	return axis

@app.route('/map')
def map():
	return render_template('map.html')

@app.route('/introduction')
def introduction():
	return render_template('introduction.html')

@app.route('/test')
def test():
	return render_template('test.html')

@app.errorhandler(404)
def page_not_found(e):
	return render_template('404.html'), 404

@app.errorhandler(500)
def internal_server_error(e):
	return render_template('index.html')

if __name__ == "__main__":
	app.run()
