int main() {
	#pragma acc parallel loop
    for (int i = 0; i < 100; i++) /*<<<<< 3,5,4,10,fail*/
        ;
}
