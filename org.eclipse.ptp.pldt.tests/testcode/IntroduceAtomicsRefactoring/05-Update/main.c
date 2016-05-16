#include <stdio.h>

int main() {
	int a = 1;
	#pragma acc parallel loop
	for (int i = 0; i < 10; i++) {
		a++;
	}
	printf("%d\n", a);
}
