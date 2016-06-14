int main() {

	int array[10][20];

	for (int i = 0; i < 10; i++) { /*<<<<< 5,1,13,1,3,2,pass*/
		for (int j = 0; j < 20; j++) {
			if (i % 2 == 0) {
				array[i][j] = 0;
			} else {
				array[i][j] = 1;
			}
		}
	}

	return 0;
}
