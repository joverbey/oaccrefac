int main() {

	int array[10][20], n = 10, m = 20;

	for (int i = 0; i < n; i++) { /*<<<<< 5,1,10,1,3,2,pass*/
		for (int j = 0; j < m; j++) {
			array[i][j] = 0;
		}
	}

	return 0;
}
