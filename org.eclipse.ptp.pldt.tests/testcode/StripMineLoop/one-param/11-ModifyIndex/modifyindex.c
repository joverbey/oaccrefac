int main() {

	int N = 100;
	int a[N];
	for (int i = 0; i < N; i++) { /*<<<<< 5,1,8,3,2,fail*/
		a[i++] = 0;
	}

}
