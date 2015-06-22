//LEGEND:
// Start line, Start char, End line, End char, Strip Depth, Strip Factor, Propagate Interchange, pass/fail
int main() {

	int array[10][20][30][4];
	for (int i = 0; i < 10; i++) {
		for (int j = 0; j < 20; j++) { /*<<<<< 7,1,13,3,2,2,-1,pass*/
			for (int k = 0; k < 30; k++) {
				for (int m = 0; m < 4; m++) {
					array[i][j][k][m] = 0;
				}
			}
		}
	}

	return 0;
}
