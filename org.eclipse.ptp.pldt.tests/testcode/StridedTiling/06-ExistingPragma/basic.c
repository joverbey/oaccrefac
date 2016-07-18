int main() {

	int N = 12;
	int a[N];
#pragma acc kernels loop
	for (int i = 0; i < N; i++) {/*<<<<< 5,1,7,6,4,pass*/
		a[i] = 0;
	}

}
