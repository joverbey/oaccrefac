int main() {

	int array[10][20][30];

	for (int i = 0; i < 10; i++) { /*<<<<< 5,1,12,1,2,2,pass*/
		for (int j = 0; j < 20; j++) {
			for (int k = 0; k < 30; k++) {
				array[i][j][k] = 0;
			}
		}
	}

	return 0;
}
