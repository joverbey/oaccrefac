int main() {
	int N = 100;
    int a[N], b[N], c[N], d[N];
    for (int i = 0; i < N; i++) { /*<<<<< 4,5,12,34,pass*/
    	int c = 10;
        a[i] = c;
        b[i] = 6;
    }
    for (int i = 0; i < N; i++) {
        b[i] = 7;
        d[i] = 8;
    }
}
