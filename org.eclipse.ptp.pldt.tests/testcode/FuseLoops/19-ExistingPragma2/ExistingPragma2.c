int main() {
	int a, b, c, d;
#pragma acc parallel loop
    for (int i = 0; i < 100; i++) { /*<<<<< 4,5,4,34,pass*/
        a = 5;
        b = 6;
    }
#pragma acc parallel loop
    for (int i = 0; i < 100; i++) {
        c = 7;
        d = 8;
    }
}
