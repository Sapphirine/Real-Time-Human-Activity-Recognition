__author__ = 'Tyler_Yan'

import threading
import time
from urllib.request import urlopen
import pandas as pd
import numpy as np

map = {'1.0': 'SITING',
       '2.0': 'STANDING',
       '3.0': 'RUNNING',
       '4.0': 'WALKING',
       '5.0': 'UPSTAIRS',
       '6.0': 'DOWNSTAIRS'}

def process(ID, google):

    conn = urlopen("http://52.6.200.164/get.php", data=ID.encode('ascii'))
    msg = conn.read().decode('utf-8')
    info = msg.split("\n")
    # print(info[0])

    xyz = pd.read_json(info[0])
    print(len(xyz))
    axis = pd.DataFrame.as_matrix(xyz)

    if len(axis) != 50:

        return

    feature = np.zeros(49)
    dim = 49
    for i in range(3):
        feature[i * (dim-1)/3] = np.mean(axis[:, i])
        feature[i * (dim-1)/3 + 1] = np.std(axis[:, i])
        feature[i * (dim-1)/3 + 2] = np.mean(np.diff(axis[:, i]))
        Max = np.amax(axis[:, i])
        Min = np.amin(axis[:, i])
        diff = Max - Min
        feature[i * (dim-1)/3 + 3] = Max
        feature[i * (dim-1)/3 + 4] = Min
        feature[i * (dim-1)/3 + 5] = diff
        step = np.arange(Min, Max, diff/11)
        if len(step) != 11:
            step = step[0:11]
        hist = np.histogram(axis[:, i], bins=step)
        feature[i * (dim-1)/3 + 6:i * (dim-1)/3 + (dim-1)/3] = hist[0]/sum(hist[0])

    feature[48] = np.mean(np.sqrt(np.square(axis[:, 0]) + np.square(axis[:, 1]) + np.square(axis[:, 2])))

    from sklearn.externals import joblib
    clf = joblib.load('svm.pkl')
    result = clf.predict(feature)
    print(map[str(result[0])])

    import requests
    data = '{\"name\": \"' + ID + '\", \"value\": \"' + map[str(result[0])] + '\", \"google\": \"' + google + '\"}'
    r = requests.post("http://52.6.200.164/result.php", data=data)
    print(r.status_code, r.reason)

while True:

    user_table = urlopen("http://52.6.200.164/getUsernames.php")
    getuser = user_table.read().decode('utf-8')
    getinfo = getuser.split("\n")
    # print(getinfo[0])
    user = pd.read_json(getinfo[0])

    for index in range(0, len(user)):
        print(user['username'].iloc[index])
        print(user['google'].iloc[index])
        t = threading.Thread(target=process, args=(user['username'].iloc[index], user['google'].iloc[index]))
        t.start()

    time.sleep(1)