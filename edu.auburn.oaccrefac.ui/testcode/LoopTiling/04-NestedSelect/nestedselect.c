
int main() {

	int array[10][20][30][4];
	for (int i = 0; i < 10; i++) {
		for (int j = 0; j < 20; j++) { /*<<<<< 6,1,12,3,2,2,-1*/
			for (int k = 0; k < 30; k++) {
				for (int m = 0; m < 4; m++) {
					array[i][j][k][m] = 0;
				}
			}
		}
	}

	return 0;
}
