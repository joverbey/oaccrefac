// Unexpected behavior for kernels loop because second
// loop carries dependence

#define N 10

int main() {
	int a[N];
	for (int i = 0; i < N; i++) {
		a[i] = 0;
	}
	a[0] = 1;
    for (int i = 1; i < N; i++) /*<<<<< 12,5,15,10,fail*/
    {
    	a[i] = a[i-1] + 1;
    }
}
