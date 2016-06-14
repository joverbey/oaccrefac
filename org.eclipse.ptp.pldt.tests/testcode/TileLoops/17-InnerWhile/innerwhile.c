int main() {

	int array[10][20];

	for (int i = 0; i < 10; i++) { /*<<<<< 5,1,12,1,3,2,pass*/
		for (int j = 0; j < 20; j++) {
			array[i][j] = 0;
			while (1) {
				break;
			}
		}
	}

	return 0;
}
