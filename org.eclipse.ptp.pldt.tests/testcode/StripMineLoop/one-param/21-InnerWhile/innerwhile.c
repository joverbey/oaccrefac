int main() {

	int N = 100;
	int a[N];
	for (int i = 0; i < N; i++) { /*<<<<< 5,1,12,3,2,pass*/
		a[i] = 0;
		int j = 0;
		while (j++ < 5) {
			a[i]++;
		}
	}

}
