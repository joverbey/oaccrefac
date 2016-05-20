#include <stdio.h>

int main() {
	#pragma acc parallel loop
	for (int i = 0; i < 10; i++) {
		int a = 1;
		#pragma acc atomic write
		a = i + 1;
	}
	printf("%d\n", a);
}
