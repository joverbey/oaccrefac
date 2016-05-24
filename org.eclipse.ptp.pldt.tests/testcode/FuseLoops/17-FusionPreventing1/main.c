int main() {

	int a[100][100], b[100][100], c[100][100], d[100][100];

	for(int i = 0; i < 100; i++) {
		for(int j = 0; j < 100; j++) { /*<<<<< 6,2,8,2,fail*/
			a[i][j] = b[i][j] + d[i][j];
		}
		for(int j = 0; j < 100; j++) {
			b[i][j] = a[i][j+1] - d[i][j];
		}
	}
}

