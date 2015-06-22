//LEGEND:
// Start line, Start char, End line, End char, Strip Depth, Strip Factor, Propagate Interchange, pass/fail
int main() {

	int array[10][20];
	for (int i = 0; i < 10; i++) { /*<<<<< 6,1,12,1,1,2,-1,pass*/
		for (int j = 0; j < 20; j++) {
			printf("%d %d\n", i, j);
			array[i][j] = 0;
		}
	}

	return 0;
}
