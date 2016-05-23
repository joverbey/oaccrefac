int main() {
	int val = 1;
    int *x = &val;
#pragma acc parallel loop
    for (int i = 0; i < 100; i++) {
        (*x)++; /*<<<<< 6,0,7,0,pass*/
    }
}
