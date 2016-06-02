int main() {
	int val = 1;
    int *x = &val;
#pragma acc parallel loop
    for (int i = 0; i < 100; i++) {
        int v = 0;
        v = *x; /*<<<<< 7,0,8,0,pass*/
    }
}
