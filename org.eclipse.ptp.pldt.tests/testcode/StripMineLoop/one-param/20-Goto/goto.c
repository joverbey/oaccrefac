int main() {

	int N = 100;
	int a[N];
	for (int i = 0; i < N; i++) { /*<<<<< 5,1,9,3,2,fail*/
		a[i] = 0;
		goto LOOP;
	}

LOOP: a[5] = 7;
}
