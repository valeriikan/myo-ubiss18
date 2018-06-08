import matplotlib.pyplot as plt
from data import trials
from sklearn.manifold import TSNE
from sklearn.decomposition import PCA


X = [t.X() for t in trials]
y = [t.y() for t in trials]


x = TSNE(n_components=1).fit_transform(X)
#x = PCA(n_components=1).fit_transform(X)

x = [x0/1000.0 for x0 in x]

print x
print y


plt.title('Feature space vs alcohol level')

plt.ylabel('Blood alcohol level (g/dL alcohol)')
plt.xlabel('1st TSNE component')


plt.scatter(x ,y)
plt.show()