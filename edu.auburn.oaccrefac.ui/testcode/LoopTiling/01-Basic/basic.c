
int main() {

	int array[10][20];
	for (int i = 0; i < 10; i++) { /*<<<<< 5,1,11,1,1,2,-1*/
		for (int j = 0; j < 20; j++) {
			printf("%d %d\n", i, j);
			array[i][j] = 0;
		}
	}

	return 0;
}
