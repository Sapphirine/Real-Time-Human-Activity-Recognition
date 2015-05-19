__author__ = 'Tyler_Yan'

import numpy as np
state = ['SIT', 'STAND', 'RUN', 'WALK', 'DOWN', 'UP']
window_size = 50
Xtrain = Ytrain = Ztrain = Xtest = Ytest = Ztest = label_train = label_test = []

for j in range(len(state)):
    data = np.loadtxt(state[j])
    X = np.reshape(data[:, 0], (len(data)/window_size, window_size))
    Y = np.reshape(data[:, 1], (len(data)/window_size, window_size))
    Z = np.reshape(data[:, 2], (len(data)/window_size, window_size))
    label = data[np.arange(0, len(data), 50), 3]
    seed = np.random.permutation(len(X))

    if j == 0:
        Xtrain = X[seed[0: 0.7 * len(seed)], :]
        Ytrain = Y[seed[0: 0.7 * len(seed)], :]
        Ztrain = Z[seed[0: 0.7 * len(seed)], :]
        label_train = label[seed[0: 0.7 * len(seed)]]

        Xtest = X[seed[0.7 * len(seed): len(seed)], :]
        Ytest = Y[seed[0.7 * len(seed): len(seed)], :]
        Ztest = Z[seed[0.7 * len(seed): len(seed)], :]
        label_test = label[seed[0.7 * len(seed): len(seed)]]

    else:
        Xtrain = np.concatenate((Xtrain, X[seed[0: 0.7 * len(seed)], :]), axis=0)
        Ytrain = np.concatenate((Ytrain, Y[seed[0: 0.7 * len(seed)], :]), axis=0)
        Ztrain = np.concatenate((Ztrain, Z[seed[0: 0.7 * len(seed)], :]), axis=0)
        label_train = np.concatenate((label_train, label[seed[0: 0.7 * len(seed)]]), axis=0)
        Xtest = np.concatenate((Xtest, X[seed[0.7 * len(seed): len(seed)], :]), axis=0)
        Ytest = np.concatenate((Ytest, Y[seed[0.7 * len(seed): len(seed)], :]), axis=0)
        Ztest = np.concatenate((Ztest, Z[seed[0.7 * len(seed): len(seed)], :]), axis=0)
        label_test = np.concatenate((label_test, label[seed[0.7 * len(seed): len(seed)]]), axis=0)


def feature_extraction(X, Y, Z):
    axis = [X, Y, Z]
    dim = 49
    feature = np.zeros((len(X), dim))
    for i in range(3):
        feature[:, i * (dim-1)/3] = np.mean(axis[i], 1)
        feature[:, i * (dim-1)/3 + 1] = np.std(axis[i], 1)
        feature[:, i * (dim-1)/3 + 2] = np.mean(np.diff(axis[i]), 1)
        Max = np.amax(axis[i], axis=1)
        Min = np.amin(axis[i], axis=1)
        diff = Max - Min
        feature[:, i * (dim-1)/3 + 3] = Max
        feature[:, i * (dim-1)/3 + 4] = Min
        feature[:, i * (dim-1)/3 + 5] = diff

        for j in range(len(axis[i])):
            # print(j)
            step = np.arange(Min[j], Max[j], diff[j]/11)
            if len(step) == 12:
                step = step[0:11]
            hist = np.histogram(axis[i][j, :], bins=step)
            feature[j, i * (dim-1)/3 + 6:i * (dim-1)/3 + (dim-1)/3] = hist[0]/sum(hist[0])

    feature[:, 48] = np.mean(np.sqrt(np.square(X) + np.square(Y) + np.square(Z)), 1)
    return feature

train = feature_extraction(Xtrain, Ytrain, Ztrain)
test = feature_extraction(Xtest, Ytest, Ztest)


from sklearn.externals import joblib
########### bayes ############
confusion_matrix = np.zeros((6, 6))
from sklearn.naive_bayes import GaussianNB
clf = GaussianNB()
clf.fit(train, label_train)
print("Bayes Accuracy : ")

joblib.dump(clf, 'bayes.pkl')
print(clf.score(test, label_test))

result = clf.predict(test)
for i in range(len(result)):
    confusion_matrix[label_test[i] - 1, result[i] - 1] += 1
print(confusion_matrix)
acc = np.zeros(6)
for j in range(6):
    acc[j] = confusion_matrix[j][j]/sum(confusion_matrix[j, :])
print(acc)

########### kNN ############
confusion_matrix = np.zeros((6, 6))
from sklearn.neighbors import KNeighborsClassifier
neigh = KNeighborsClassifier(n_neighbors=8)
neigh.fit(train, label_train)
joblib.dump(neigh, 'kNN.pkl')

print("kNN Accuracy : ")
print(neigh.score(test, label_test))

result = neigh.predict(test)
for i in range(len(result)):
    confusion_matrix[label_test[i] - 1, result[i] - 1] += 1
print(confusion_matrix)

acc = np.zeros(6)
for j in range(6):
    acc[j] = confusion_matrix[j][j]/sum(confusion_matrix[j, :])
print(acc)

########### svm ##############
confusion_matrix = np.zeros((6, 6))
from sklearn.svm import SVC
clf = SVC()
clf.fit(train, label_train)
joblib.dump(clf, 'svm.pkl')

print("SVM Accuracy : ")
print(clf.score(test, label_test))

result = clf.predict(test)
for i in range(len(result)):
    confusion_matrix[label_test[i] - 1, result[i] - 1] += 1
print(confusion_matrix)

acc = np.zeros(6)
for j in range(6):
    acc[j] = confusion_matrix[j][j]/sum(confusion_matrix[j, :])
print(acc)
