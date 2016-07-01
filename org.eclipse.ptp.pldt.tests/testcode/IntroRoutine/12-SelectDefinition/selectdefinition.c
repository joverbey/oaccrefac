int main() {
    for (int i = 0; i < 100; i++){
        int a = foo(3);
    }
}

int foo(int n) { /*<<<<< 6,1,10,6,pass*/
	return bar();
}

int bar() {
#pragma acc parallel loop vector
	for (int i = 0; i < 100; i++) {

	}
	return 1;
}

#pragma acc routine gang
int nonFunction() {
	int a = foo(6);
	return a;
}
