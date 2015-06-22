
int main() {

	int array[10][20][30];
	for (int i = 0; i < 10; i++) { /*<<<<< 5,1,13,1,2,2,-1*/
		for (int j = 0; j < 20; j++) {
			for (int k = 0; k < 30; k++) {
				printf("%d %d %d\n", i, j, k);
				array[i][j][k] = 0;
			}
		}
	}

	return 0;
}
