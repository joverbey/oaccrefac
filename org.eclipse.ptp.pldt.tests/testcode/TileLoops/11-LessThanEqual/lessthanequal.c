int main() {

	int array[10][20];

	for (int i = 0; i <= 9; i++) { /*<<<<< 4,1,10,1,3,2,pass*/
		for (int j = 0; j <= 19; j++) {
			array[i][j] = 0;
		}
	}

	return 0;
}
