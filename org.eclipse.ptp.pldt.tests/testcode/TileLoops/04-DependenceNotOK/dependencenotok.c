int main() {

	int a[10][20];

	for (int i = 0; i < 10; i++) { /*<<<<< 5,1,10,1,3,2,fail*/
		for (int j = 0; j < 20; j++) {
			a[i][j] = a[i-1][j+1];
		}
	}

	return 0;
}
