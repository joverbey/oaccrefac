void main() {

//	int A[100][300][200] = {0};
	int A[53] = {0};
	int B[100][300][200] = {0};

//	for(int i = 1; i < 100; i++) {
//		for(int j = 1; j < 300; j++) {
//			for(int k = 1; k < 200; k++) {
//				A[i][j][k] = B[i-1][j-1][k-1] + 1;
//			}
//		}
//	}

//	// should fail, no binary expression on right?
//	for(int i = 0; i < 10; i++) {
//		A[5*i+3] = A[5*i];
//	}
//
	// should return false - no dependence
	for(int i = 0; i < 3; i++) {
		A[i] = A[i+5] + 0;
	}

//	for(int i = 0; i < 1; i++);

//	for(int i = 1; i < 9; i++) {
//		for(int j = 1; j < 9-i+1; j++) {
//			b[i][j] = b[j][n-i] + 0;
//		}
//	}

}
