
int main() {

	int array[10][20];
	for (int j_0 = 0; j_0 < 20; j_0 += 2) {
		for (int i = 0; i < 10; i++) {
			for (int j = j_0; (j < j_0 + 2 && j < 20); j++) {
				printf("%d %d\n", i, j);
				array[i][j] = 0;
			}
		}
	}

	return 0;
}
