int main() {
	int N = 100;
    int a[N], b[N], c[N], d[N];
    for (int i = 0; i < 100; i++) { /*<<<<< 4,5,4,34,pass*/
        a[i] = 5;
        b[i] = 6;
        for (int j = 0; j < i; j++) {
        	a[i]++;
        	b[i] = b[i-1];
        }
    }
    for (int i = 0; i < 100; i++) {
        c[i] = 7;
        d[i] = 8;
    }
}
