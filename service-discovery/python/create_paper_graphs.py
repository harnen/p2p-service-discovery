import sys
import matplotlib.pyplot as plt
import numpy as np
import matplotlib

font = {'family' : 'normal',
        'weight' : 'bold',
        'size'   : 16}

matplotlib.rc('font', **font)
matplotlib.rcParams['pdf.fonttype'] = 42
matplotlib.rcParams['ps.fonttype'] = 42

def f(ax, x, y, y_min, y_max, color, label):
	ax.plot(x, y, '-', color=color)
	ax.fill_between(x, y_min, y_max, alpha=0.2, color=color, label=label)
	ax.spines['right'].set_visible(False)
	ax.spines['top'].set_visible(False)
	ax.legend()
	


def main() -> int:
	fig, ax = plt.subplots(figsize=(10, 4))
	x = np.linspace(0, 10, 11)
	y = [0, 0.4, 6.8, 6.3, 7.2, 8.1, 8.1,  7.9, 9.9, 10.1, 8.5]
	y_max = [x+2 for x in y]
	y_min = [x-2 for x in y]
	f(ax, x, y, y_min, y_max, 'r', 'Discv4')

	y = [3.9, 4.4, 10.8, 10.3, 11.2, 13.1, 14.1,  9.9, 13.9, 15.1, 12.5]
	y_max = [x+2 for x in y]
	y_min = [x-2 for x in y]
	f(ax, x, y, y_min, y_max, 'g', 'Discv5')

	y = [9.9, 12.4, 15.8, 15.3, 16.2, 17.1, 17.1,  15.9, 17.9, 17.1, 17.5]
	y_max = [x+2 for x in y]
	y_min = [x-2 for x in y]
	f(ax, x, y, y_min, y_max, 'b', 'DHT')

	plt.show()
	return 0

if __name__ == '__main__':
    sys.exit(main())  # next section explains the use of sys.exit