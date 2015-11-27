/* Copyright (c) 2013 The University of Edinburgh. */

/* Licensed under the Apache License, Version 2.0 (the "License"); */
/* you may not use this file except in compliance with the License. */
/* You may obtain a copy of the License at */

/*     http://www.apache.org/licenses/LICENSE-2.0 */

/* Unless required by applicable law or agreed to in writing, software */
/* distributed under the License is distributed on an "AS IS" BASIS, */
/* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. */
/* See the License for the specific language governing permissions and */
/* limitations under the License. */

#include <stdio.h>
#include <unistd.h>
#include <string.h>
#include <math.h>
#include <omp.h>
#include <sys/time.h>
#include "common.h"
#include "main.h"
#include <stdlib.h>

#ifdef _OPENACC
#include <openacc.h>
#endif


/*
 * Default problem limits.
 */
#ifndef TOL
#define TOL 1e-14
#define FDTOL 1e-12 /* See fdtd2d for justification. */
#endif
#ifndef T_MAX
#define T_MAX 50
#endif

double gettime() {
    struct timeval t;
    gettimeofday(&t, NULL);
    return t.tv_sec + t.tv_usec*1.0e-6;
}


double twomm(){

  extern unsigned int datasize;
  int i = 0;
  int j = 0;
  int k = 0;
  double tmp = 0;
  double t_start = 0;
  double t_end = 0;
  int flag = 0;

  /*
   * We need to hold 5 arrays on the GPU.
   * Make them all square to fit within datasize.
   */
  int n = 512;//sqrt((int)((datasize/sizeof(double))/5));

  double* C = (double*)malloc(n*n * sizeof(double));
  double* A = (double*)malloc(n*n * sizeof(double));
  double* B = (double*)malloc(n*n * sizeof(double));
  double* D = (double*)malloc(n*n * sizeof(double));
  double* E = (double*)malloc(n*n * sizeof(double));
  double* H = (double*)malloc(n*n * sizeof(double));

  if (A==NULL||B==NULL||C==NULL||D==NULL||E==NULL||H==NULL){
    /* Something went wrong in the memory allocation here, fail gracefully */
    return(-10000);
  }

  for (i = 0; i < n; i++){ /*<<<<< 77, 1, 81, 4, loop1outer*/
    for (j = 0; j < n; j++){ /*<<<<< 78, 1, 80, 6, loop1inner*/
      A[i*n+j] = rand()/ (1.0 + RAND_MAX);
    }
  }
  for (i = 0; i < n; i++){ /*<<<<< 82, 1, 86, 4, loop2outer*/
    for (j = 0; j < n; j++){ /*<<<<< 83, 1, 85, 6, loop2inner*/
      B[i*n+j] = rand()/ (1.0 + RAND_MAX);
    }
  }
  for (i = 0; i < n; i++){ /*<<<<< 87, 1, 91, 4, loop3outer*/
    for (j = 0; j < n; j++){ /*<<<<< 88, 1, 90, 6, loop3inner*/
      D[i*n+j] = rand()/ (1.0 + RAND_MAX);
    }
  }

  /*
   * Host code for reference.
   * Store result in H.
   */
//  for (i = 0; i < n; i++){
//    for (j = 0; j < n; j++){
//      C[i*n+j] = 0;
//      for (k = 0; k < n; k++){
//        C[i*n+j] += A[i*n+k] * B[k*n+j];
//      }
//    }
//  }
//
//  for (i = 0; i < n; i++){
//    for (j = 0; j < n; j++){
//      H[i*n+j] = 0;
//      for (k = 0; k < n; k++)
//        H[i*n+j] += C[i*n+k] * D[k*n+j];
//    }
//  }

  /* Accelerator code */


  /* E := A*B*C */
  t_start = gettime();
#pragma acc data copyin(A[0:n*n],B[0:n*n],D[0:n*n], n), copyout(E[0:n*n]), create(C[0:n*n], tmp,i,j,k)
  {

#pragma acc parallel loop private(tmp)
    for (i = 0; i < n; i++){ /*<<<<< 123, 1, 133, 6, loop14outer*/
#pragma acc loop private(tmp)
      for (j = 0; j < n; j++){ /*<<<<< 125, 1, 132, 8, loop4inner*/
	tmp = 0;
#pragma acc loop reduction(+:tmp)
        for (k = 0; k < n; k++){ /*<<<<< 78, 1, 80, 10, loop4inner2*/
	  tmp += A[i*n+k] * B[k*n+j];
        }
	C[i*n+j] = tmp;
      }
    }


#pragma acc parallel loop private(tmp)
    for (i = 0; i < n; i++){ /*<<<<< 137, 1, 147, 6, loop5outer*/
#pragma acc loop private(tmp)
      for (j = 0; j < n; j++){ /*<<<<< 139, 1, 80, 10, loop5inner*/
	tmp = 0;
#pragma acc loop reduction(+:tmp)
        for (k = 0; k < n; k++){ /*<<<<< 142, 1, 144, 10, loop5inner2*/
          tmp += C[i*n+k] * D[k*n+j];
        }
        E[i*n+j] = tmp;
      }
    }

  }/* end data */

  t_end = gettime();

  /* Compare Host + Device code. */

//  for (i = 0; i < n; i++){
//    for (j = 0; j < n; j++){
//      if (  fabs(H[i*n+j] - E[i*n+j])/H[i*n+j] > TOL ){
//        flag = 1;
//      }
//    }
//  }

  /* Free malloc'd memory to prevent leaks */
  free(A);
  free(B);
  free(C);
  free(D);
  free(E);
  free(H);

  if (flag == 0) return (t_end-t_start);
  else return(-11000);

}


double threemm(){

  extern unsigned int datasize;
  int i = 0;
  int j = 0;
  int k = 0;
  double t_start = 0;
  double t_end = 0;
  int flag = 0;

  /* Seven arrays on the device here. */
  int n = 512;// sqrt((int)((datasize/sizeof(double))/7));

  double* A = (double*)malloc(n*n * sizeof(double));
  double* B = (double*)malloc(n*n * sizeof(double));
  double* C = (double*)malloc(n*n * sizeof(double));
  double* D = (double*)malloc(n*n * sizeof(double));
  double* E = (double*)malloc(n*n * sizeof(double));
  double* F = (double*)malloc(n*n * sizeof(double));
  double* G = (double*)malloc(n*n * sizeof(double));
  double* H = (double*)malloc(n*n * sizeof(double));

  if (A==NULL||B==NULL||C==NULL||D==NULL||E==NULL||F==NULL||G==NULL||H==NULL){
    /* Something went wrong in the memory allocation here, fail gracefully */
    return(-10000);
  }


  for (i = 0; i < n; i++){ /*<<<<< 205, 1, 216, 4, loop6outer*/
    for (j = 0; j < n; j++){ /*<<<<< 206, 1, 215, 6, loop6inner*/
      A[i*n+j] = rand()/ (1.0 + RAND_MAX);
      B[i*n+j] = rand()/ (1.0 + RAND_MAX);
      C[i*n+j] = rand()/ (1.0 + RAND_MAX);
      D[i*n+j] = rand()/ (1.0 + RAND_MAX);
      E[i*n+j] = rand()/ (1.0 + RAND_MAX);
      F[i*n+j] = rand()/ (1.0 + RAND_MAX);
      G[i*n+j] = rand()/ (1.0 + RAND_MAX);
      H[i*n+j] = rand()/ (1.0 + RAND_MAX);
    }
  }

  /* Host version for reference. */
  /* E := A*B */
  for (i = 0; i < n; i++) { /*<<<<< 220, 1, 227, 4, loop7outer*/
    for (j = 0; j < n; j++){ /*<<<<< 221, 1, 226, 6, loop7inner*/
      E[i*n+j] = 0;
      for (k = 0; k < n; k++){ /*<<<<< 223, 1, 225, 8, loop7inner2*/
        E[i*n+j] += A[i*n+k] * B[k*n+j];
      }
    }
  }

  /* F := C*D */
  for (i = 0; i < n; i++) { /*<<<<< 230, 1, 237, 4, loop8outer*/
    for (j = 0; j < n; j++) { /*<<<<< 231, 1, 236, 6, loop8inner*/
      F[i*n+j] = 0;
      for (k = 0; k < n; k++){ /*<<<<< 233, 1, 235, 8, loop8inner2*/
        F[i*n+j] += C[i*n+k] * D[k*n+j];
      }
    }
  }

  /* G := E*F */
  for (i = 0; i < n; i++) { /*<<<<< 240, 1, 247, 4, loop9outer*/
    for (j = 0; j < n; j++) { /*<<<<< 241, 1, 246, 6, loop9inner*/
      H[i*n+j] = 0;
      for (k = 0; k < n; k++){ /*<<<<< 243, 1, 245, 8, loop9inner2*/
        H[i*n+j] += E[i*n+k] * F[k*n+j];
      }
    }
  }



  t_start = gettime();
#pragma acc data copyin(A[0:n*n],B[0:n*n],C[0:n*n],D[0:n*n]), create(E[0:n*n],F[0:n*n]), copyout(G[0:n*n])
  {
#pragma acc parallel loop
    for (i = 0; i < n; i++) { /*<<<<< 255, 1, 263, 6, loop10outer*/
#pragma acc loop
      for (j = 0; j < n; j++){ /*<<<<< 257, 1, 262, 8, loop10inner*/
        E[i*n+j] = 0;
        for (k = 0; k < n; k++){ /*<<<<< 259, 1, 261, 10, loop10inner2*/
          E[i*n+j] += A[i*n+k] * B[k*n+j];
        }
      }
    }

    /* F := C*D */
#pragma acc parallel loop
    for (i = 0; i < n; i++) { /*<<<<< 267, 1, 275, 6, loop11outer*/
#pragma acc loop
      for (j = 0; j < n; j++) { /*<<<<< 269, 1, 274, 8, loop11inner*/
        F[i*n+j] = 0;
        for (k = 0; k < n; k++){ /*<<<<< 271, 1, 273, 10, loop11inner2*/
          F[i*n+j] += C[i*n+k] * D[k*n+j];
        }
      }
    }

    /* G := E*F */
#pragma acc parallel loop
    for (i = 0; i < n; i++) { /*<<<<< 279, 1, 287, 6, loop12outer*/
#pragma acc loop
      for (j = 0; j < n; j++) { /*<<<<< 281, 1, 286, 8, loop12inner*/
        G[i*n+j] = 0;
        for (k = 0; k < n; k++){ /*<<<<< 283, 1, 285, 10, loop12inner2*/
          G[i*n+j] += E[i*n+k] * F[k*n+j];
        }
      }
    }

  } /* end data */
  t_end=gettime();



  for (i = 0; i < n; i++){ /*<<<<< 294, 1, 300, 4, loop13outer*/
    for (j = 0; j < n; j++){ /*<<<<< 295, 1, 299, 6, loop13inner*/
      if (  fabs(H[i*n+j] - G[i*n+j])/H[i*n+j] > TOL ){
        flag = 1;
      }
    }
  }

  /* Free malloc'd memory to prevent leaks */
  free(A);
  free(B);
  free(C);
  free(D);
  free(E);
  free(F);
  free(G);
  free(H);

  if (flag == 0) return (t_end-t_start);
  else return(-11000);

}


double atax(){

  int i = 0;
  int j = 0;
  extern unsigned int datasize;
  int n = 4096;//sqrt((int)datasize/sizeof(double));
  double t_end = 0;
  double t_start = 0;
  int flag = 0;

  double *A = NULL;
  double *x = NULL;
  double *y = NULL;
  double *ya = NULL;
  double *tmp = NULL;
  double tmpscalar = 0;

  A = (double *)malloc(sizeof(double)*n*n);
  x = (double *)malloc(sizeof(double)*n);
  y = (double *)malloc(sizeof(double)*n);
  ya = (double *)malloc(sizeof(double)*n);
  tmp = (double *)malloc(sizeof(double)*n);

  if(A==NULL||x==NULL||y==NULL||ya==NULL||tmp==NULL){
    /* Something went wrong in the memory allocation here, fail gracefully */
    return(-10000);
  }



  for (i = 0; i < n; i++){ /*<<<<< 348, 1, 354, 4, loop14outer*/
    x[i] = rand() / (1.0 + RAND_MAX);
    y[i] = 0;
    for (j = 0; j < n; j++){ /*<<<<< 351, 1, 353, 6, loop14inner*/
      A[i*n+j] = rand() / (1.0 + RAND_MAX);
    }
  }

  for (i = 0; i < n; i++){ /*<<<<< 356, 1, 362, 4, loop15outer*/
    tmpscalar = 0;
    for (j = 0; j < n; j++){ /*<<<<< 358, 1, 360, 6, loop15inner*/
      tmpscalar += A[i*n+j] * x[j];
    }
    tmp[i] = tmpscalar;
  }


  for (j = 0; j < n; j++){ /*<<<<< 365, 1, 371, 4, loop16outer*/
    tmpscalar = 0;
    for (i = 0; i < n; i++){ /*<<<<< 367, 1, 369, 6, loop16inner*/
      tmpscalar += A[i*n+j] * tmp[i];
    }
    y[j] = tmpscalar;
  }

  tmpscalar = 0;

  t_start = gettime();
#pragma acc data copyin(A[0:n*n], x[0:n]), create(tmp[0:n]), copyout(ya[0:n])
  {

#pragma acc parallel loop private(tmpscalar)
    for (i = 0; i < n; i++){ /*<<<<< 380, 1, 387, 6, loop17outer*/
      tmpscalar = 0;
#pragma acc loop reduction(+:tmpscalar)
      for (j = 0; j < n; j++){ /*<<<<< 383, 1, 385, 8, loop17inner*/
        tmpscalar += A[i*n+j] * x[j];
      }
      tmp[i] = tmpscalar;
    }

#pragma acc parallel loop private(tmpscalar)
    for (j = 0; j < n; j++){ /*<<<<< 390, 1, 398, 6, loop18outer*/
      tmpscalar = 0;
#pragma acc loop reduction(+:tmpscalar)
      for (i = 0; i < n; i++){ /*<<<<< 393, 1, 395, 8, loop18inner*/
        tmpscalar += A[i*n+j] * tmp[i];
      }
      ya[j] = tmpscalar;

    }

  } /* end data */
  t_end = gettime();

  for (j = 0; j < n; j++){ /*<<<<< 403, 1, 407, 4, loop19outer*/
    if (  fabs(y[j] - ya[j])/y[j] > TOL ){
      flag = 1;
    }
  }

  /* Free malloc'd memory to prevent leaks */
  free(A);
  free(x);
  free(y);
  free(ya);
  free(tmp);

  if (flag == 0) return (t_end-t_start);
  else return(-11000);

}



double bicg(){

  int i = 0;
  int j = 0;
  double tmp = 0;

  extern unsigned int datasize;
  int n = 4096;//sqrt(datasize/sizeof(double));
  double t_start = 0;
  double t_end = 0;
  int flag = 0;
  double *A = (double *)malloc(sizeof(double)*n*n);
  double *r = (double *)malloc(sizeof(double)*n);
  double *s = (double *)malloc(sizeof(double)*n);
  double *sh = (double *)malloc(sizeof(double)*n);
  double *q = (double *)malloc(sizeof(double)*n);
  double *qh = (double *)malloc(sizeof(double)*n);
  double *p = (double *)malloc(sizeof(double)*n);

  if(A==NULL||r==NULL||s==NULL||sh==NULL||q==NULL||qh==NULL||p==NULL){
    /* Something went wrong in the memory allocation here, fail gracefully */
    return(-10000);
  }

  for (i = 0; i < n; i++) { /*<<<<< 447, 1, 453, 4, loop20outer*/
    r[i] = rand() / (1.0 + RAND_MAX);
    p[i] = rand() / (1.0 + RAND_MAX);
    for (j = 0; j < n; j++) { /*<<<<< 450, 1, 452, 6, loop20inner*/
      A[i*n+j] = rand() / (1.0 + RAND_MAX);
    }
  }


  t_start = gettime();
#pragma acc data copyin(A[0:n*n],r[0:n],p[0:n]), copyout(q[0:n],s[0:n])
  {

#pragma acc parallel loop private(tmp)
    for (i = 0; i < n; i++){ /*<<<<< 461, 1, 468, 6, loop21outer*/
      tmp = 0;
#pragma acc loop reduction(+:tmp)
      for (j = 0; j < n; j++){ /*<<<<< 464, 1, 466, 8, loop21inner*/
        tmp += A[i*n+j] * p[j];
      }
      q[i] = tmp;
    }

#pragma acc parallel loop private(tmp)
    for (j = 0; j < n; j++){ /*<<<<< 471, 1, 478, 6, loop22outer*/
      tmp = 0;
#pragma acc loop reduction(+:tmp)
      for (i = 0; i < n; i++){ /*<<<<< 474, 1, 478, 8, loop22inner*/
        tmp += r[i] * A[i*n+j];
      }
      s[j] = tmp;
    }


  }
  t_end = gettime();


  for (i = 0; i < n; i++){ /*<<<<< 485, 1, 491, 4, loop23outer*/
    tmp = 0;
    for (j = 0; j < n; j++){ /*<<<<< 487, 1, 489, 6, loop23inner*/
      tmp += A[i*n+j] * p[j];
    }
    qh[i] = tmp;
  }


  for (j = 0; j < n; j++){ /*<<<<< 494, 1, 500, 4, loop24outer*/
    tmp = 0;
    for (i = 0; i < n; i++){ /*<<<<< 496, 1, 498, 6, loop24inner*/
      tmp += r[i] * A[i*n+j];
    }
    sh[j] = tmp;
  }


  for (j = 0; j < n; j++){ /*<<<<< 503, 1, 508, 4, loop25outer*/
    if (fabs(qh[j] - q[j])/qh[j] > TOL ){
      flag = 1;
    }
  }

  for (j = 0; j < n; j++){ /*<<<<< 509, 1, 513, 4, loop26outer*/
    if (fabs(sh[j] - s[j])/sh[j] > TOL ){
      flag = 1;
    }
  }

 /* Free malloc'd memory to prevent leaks */
  free(A);
  free(r);
  free(s);
  free(sh);
  free(q);
  free(qh);
  free(p);

  if (flag == 0) return (t_end-t_start);
  else return(-11000);

}


double mvt(){

  extern unsigned int datasize;
  int n = 4096;//sqrt(datasize/sizeof(double));
  int i = 0;
  int j = 0;
  double tmp1 = 0;
  double tmp2 = 0;
  int flag1 = 0;
  int flag2 = 0;
  double t_end = 0;
  double t_start = 0;
  double* x1 = (double*)malloc(n*sizeof(double));
  double* x1_Gpu = (double*)malloc(n*sizeof(double));
  double* x2 = (double*)malloc(n*sizeof(double));
  double* x2_Gpu = (double*)malloc(n*sizeof(double));
  double* y1 = (double*)malloc(n*sizeof(double));
  double* y2 = (double*)malloc(n*sizeof(double));
  double *a = (double *)malloc(sizeof(double)*n*n);

  if(x1==NULL||x1_Gpu==NULL||x2==NULL||x2_Gpu==NULL||y1==NULL||y2==NULL||a==NULL){
    /* Something went wrong in the memory allocation here, fail gracefully */
    return(-10000);
  }

  /* Init */
  for (i = 0; i < n; i++){ /*<<<<< 556, 1, 566, 4, loop27outer*/
    x1[i] = (i+1) / n;
    x1_Gpu[i] = (i+1) / n;
    x2[i] = (i + 1) / n;
    x2_Gpu[i] = (i + 1) / n;
    y1[i] = (i + 3) / n;
    y2[i] = (i + 4) / n;
    for (j = 0; j < n; j++){ /*<<<<< 563, 1, 565, 4, loop27inner*/
      a[i*n+j] = ((i+1)*(j+1)) / n;
    }
  }

  /* HOST */
  for (i=0; i<n; i++){ /*<<<<< 516, 1, 575, 4, loop28outer*/
    tmp1 = 0;
    for (j=0; j<n; j++){ /*<<<<< 571, 1, 573, 6, loop28inner*/
      tmp1 += a[i*n+j] * y1[j];
    }
    x1[i] += tmp1;
  }



  for (i=0; i<n; i++){ /*<<<<< 579, 1, 585, 4, loop29outer*/
    tmp2 = 0;
    for (j=0; j<n; j++){ /*<<<<< 581, 1, 583, 6, loop29inner*/
      tmp2 += a[j*n+i] * y2[j];
    }
    x2[i] += tmp2;
  }


  /* ACCELERATOR */
  tmp1 = 0;
  tmp2 = 0;

  t_start = gettime();
#pragma acc data copyin(a[0:n*n], y2[0:n], y1[0:n]), copy(x1_Gpu[0:n], x2_Gpu[0:n])
  {

#pragma acc parallel loop private(tmp1)
    for (i=0; i<n; i++){ /*<<<<< 597, 1, 604, 6, loop30outer*/
      tmp1 = 0;
#pragma acc loop reduction(+:tmp1)
      for (j=0; j<n; j++){ /*<<<<< 600, 1, 602, 8, loop30inner*/
        tmp1 += a[i*n+j] * y1[j];
      }
      x1_Gpu[i] += tmp1;
    }


#pragma acc parallel loop private(tmp2)
    for (i=0; i<n; i++){ /*<<<<< 608, 1, 615, 6, loop31outer*/
      tmp2 = 0;
#pragma acc loop reduction(+:tmp2)
      for (j=0; j<n; j++){ /*<<<<< 611, 1, 613, 8, loop31inner*/
        tmp2 += a[j*n+i] * y2[j];
      }
      x2_Gpu[i] += tmp2;
    }


  } /* end data */
  t_end = gettime();

  /* Compare */
  for (i=0; i<n; i++){ /*<<<<< 622, 1, 626, 4, loop32outer*/
    if (fabs(x1[i] - x1_Gpu[i])/x1[i] > TOL){
      flag1 = 1;
    }
  }

  for (i=0; i<n; i++){ /*<<<<<628, 1, 632, 4, loop33outer*/
    if (fabs(x2[i] - x2_Gpu[i])/x2[i] > TOL){
      flag2 = 1;
    }
  }

 /* Free malloc'd memory to prevent leaks */
  free(a);
  free(x1);
  free(x1_Gpu);
  free(x2);
  free(x2_Gpu);
  free(y1);
  free(y2);

  if (flag1==0 && flag2==0) return (t_end-t_start);
  else return(-11000);

}


double syrk(){
  extern unsigned int datasize;
  int n = 1024;//sqrt((datasize/sizeof(double))/3);

  double alpha = 12435;
  double beta = 4546;
  int i = 0;
  int j = 0;
  int k = 0;
  double* A = (double*)malloc(n*n * sizeof(double));
  double* C = (double*)malloc(n*n * sizeof(double));
  double* CG = (double*)malloc(n*n * sizeof(double));
  double tmp = 0;
  double t_start = 0;
  double t_end = 0;
  int flag = 0;


  if(A==NULL||C==NULL||CG==NULL){
    /* Something went wrong in the memory allocation here, fail gracefully */
    return(-10000);
  }

  /* Init */
  for (i = 0; i < n; i++){ /*<<<<< 673, 1, 679, 4, loop34outer*/
    for (j = 0; j < n; j++){ /*<<<<< 674, 1, 678, 6, loop34inner*/
      A[i*n+j] = (i*j) / n;
      C[i*n+j] = (i*j + 2) / n;
      CG[i*n+j] = (i*j + 2) / n;
    }
  }

  /* HOST */
  for (i = 0; i < n; i++){/*<<<<< 682, 1, 686, 4, loop35outer*/
    for (j = 0; j < n; j++){/*<<<<< 683, 1, 685, 6, loop35inner*/
      C[i*n+j] *= beta;
    }
  }


  for (i = 0; i < n; i++){ /*<<<<< 689, 1, 695, 4, loop36outer*/
    for (j = 0; j < n; j++){ /*<<<<< 690, 1, 694, 6, loop36inner*/
      for (k = 0; k < n; k++){ /*<<<<< 691, 1, 693, 8, loop36inner2*/
        C[i*n+j] += alpha * A[i*n+k] * A[j*n+k];
      }
    }
  }


  /* ACCEL */

  t_start = gettime();
#pragma acc data copyin(A[0:n*n]), copy(CG[0:n*n])
  {
#pragma acc parallel loop
    for (i = 0; i < n; i++){ /*<<<<< 704, 1, 709, 6, loop37outer*/
#pragma acc loop
      for (j = 0; j < n; j++){ /*<<<<< 706, 1, 708, 8, loop37inner*/
        CG[i*n+j] *= beta;
      }
    }

#pragma acc parallel loop
    for (i = 0; i < n; i++){ /*<<<<< 712, 1, 721, 6, loop38outer*/
#pragma acc loop
      for (j = 0; j < n; j++){ /*<<<<< 714, 1, 720, 8, loop38inner*/
        tmp = CG[i*n+j];
        for (k = 0; k < n; k++){ /*<<<<< 716, 1, 718, 10, loop38inner2*/
          tmp += alpha * A[i*n+k] * A[j*n+k];
        }
        CG[i*n+j] = tmp;
      }
    }

  } /* end data */
  t_end = gettime();

  /* Compare */
  for (i = 0; i < n; i++){ /*<<<<< 727, 1, 733, 6, loop39outer*/
    for (j = 0; j < n; j++){ /*<<<<< 728, 1, 732, 8, loop39inner*/
      if (fabs(C[i*n+j] - CG[i*n+j])/C[i*n+j] > TOL){
        flag = 1;
      }
    }
  }

  /* Free malloc'd memory to prevent leaks */
  free(A);
  free(C);
  free(CG);

  if (flag==0) return (t_end-t_start);
  else return(-11000);

}


double covariance(){

  extern unsigned int datasize;

  int i = 0;
  int j = 0;
  int j1 = 0;
  int j2 = 0;
  int n = 512;//sqrt((datasize/sizeof(double))/2);


  double *data = (double *)malloc(n*n*sizeof(double));
  double *hdata = (double *)malloc(n*n*sizeof(double));
  double *symmat = (double *)malloc(n*n*sizeof(double));
  double *hsymmat = (double *)malloc(n*n*sizeof(double));
  double *mean = (double*)malloc(n*sizeof(double));
  double *hmean = (double*)malloc(n*sizeof(double));
  double tmp = 0;
  double t_start = 0;
  double t_end = 0;
  int flag = 0;

  if (data==NULL||hdata==NULL||symmat==NULL||hsymmat==NULL||mean==NULL||hmean==NULL){
    /* Something went wrong in the memory allocation here, fail gracefully */
    return(-10000);
  }

  /* Initialize data arrays */

  for (i = 0; i < n; i++){ /*<<<<< 775, 1, 780, 4, loop40outer*/
    for (j = 0; j < n; j++){ /*<<<<< 776, 1, 779, 6, loop40inner*/
      data[i*n+j] =  i % 12 + 2 * (j % 7);
      hdata[i*n+j] = data[i*n+j];
    }
  }



  /* Determine mean of column vectors of input data matrix */
  for (j = 0; j < n; j++){ /*<<<<< 785, 1, 791, 4, loop41outer*/
    hmean[j] = 0.0;
    for (i = 0; i < n; i++){ /*<<<<< 787, 1, 789, 6, loop41inner*/
      hmean[j] += hdata[i*n+j];
    }
    hmean[j] /= n;
  }

  /* Center the column vectors. */
  for (i = 0; i < n; i++){ /*<<<<< 794, 1, 798, 4, loop42outer*/
    for (j = 0; j < n; j++){ /*<<<<< 795, 1, 797, 6, loop42inner*/
      hdata[i*n+j] -= hmean[j];
    }
  }

  /* Calculate the n * n covariance matrix. */
  for (j1 = 0; j1 < n; j1++){ /*<<<<< 801, 1, 810, 4, loop43outer*/
    for (j2 = j1; j2 < n; j2++){ /*<<<<< 802, 1, 809, 6, loop43inner*/
      hsymmat[j1*n+j2] = 0.0;
      for (i = 0; i < n; i++){ /*<<<<< 804, 1, 806, 8, loop43inner2*/
        hsymmat[j1*n+j2] += hdata[i*n+j1] * hdata[i*n+j2];
      }
      hsymmat[j1*n+j2] /= n-1;
      hsymmat[j2*n+j1] = hsymmat[j1*n+j2];
    }
  }




  t_start = gettime();
#pragma acc data copyin(data[0:n*n]),  copyout(symmat[0:n*n]), create(mean[0:n])
  {
    /* Determine mean of column vectors of input data matrix */
#pragma acc parallel loop private(tmp)
    for (j = 0; j < n; j++){ /*<<<<< 820, 1, 827, 6, loop44outer*/
      tmp = 0;
#pragma acc loop reduction(+:tmp)
      for (i = 0; i < n; i++){ /*<<<<< 823, 1, 825, 8, loop44inner*/
        tmp += data[i*n+j];
      }
      mean[j] = tmp/n;
    }

    /* Center the column vectors. */
#pragma acc parallel loop
    for (i = 0; i < n; i++){ /*<<<<< 831, 1, 836, 6, loop45outer*/
#pragma acc loop
      for (j = 0; j < n; j++){ /*<<<<< 833, 1, 835, 8, loop45inner*/
        data[i*n+j] -= mean[j];
      }
    }

    /* Calculate the n * n covariance matrix. */
#pragma acc parallel loop
    for (j1 = 0; j1 < n; j1++){ /*<<<<< 840, 1, 851, 6, loop46outer*/
#pragma acc loop private(tmp)
      for (j2 = j1; j2 < n; j2++){ /*<<<<< 842, 1, 850, 8, loop46inner*/
        tmp = 0;
#pragma acc loop reduction(+:tmp)
        for (i = 0; i < n; i++){ /*<<<<< 845, 1, 847, 10, loop46inner2*/
          tmp += data[i*n+j1] * data[i*n+j2];
        }
        symmat[j1*n+j2] = tmp/(n-1);
        symmat[j2*n+j1] = symmat[j1*n+j2];
      }
    }


  } /* end data */
  t_end = gettime();



  /*
   * Test for a difference between host and accelerator results.
   * Use a percentage difference as there will inevitably be some bit differences
   */
  for (j1 = 0; j1 < n; j1++){ /*<<<<< 863, 1, 869, 4, loop47outer*/
    for (j2 = 0; j2 < n; j2++){ /*<<<<< 864, 1, 868, 6, loop47inner*/
      if (fabs(hsymmat[j1*n+j2] - symmat[j1*n+j2])/hsymmat[j1*n+j2] > TOL){
        flag = 1;
      }
    }
  }

  /* Free malloc'd memory to prevent leaks */
  free(data);
  free(hdata);
  free(symmat);
  free(hsymmat);
  free(mean);
  free(hmean);

  if (flag==0) return (t_end-t_start);
  else return(-11000);
}


double correlation(){

  int i = 0;
  int j = 0;
  int j1 = 0;
  int j2 = 0;
  extern unsigned int datasize;
  int n = 512;//sqrt((datasize/sizeof(double))/2);

  double eps = 0.005;
  double tmp = 0;
  double* data = (double*)malloc(n*n * sizeof(double));
  double* hdata = (double*)malloc(n*n * sizeof(double));
  double* symmat = (double*)malloc(n*n * sizeof(double));
  double* hsymmat = (double*)malloc(n*n * sizeof(double));
  double* stddev = (double*)malloc(n * sizeof(double));
  double* hstddev = (double*)malloc(n * sizeof(double));
  double* mean = (double*)malloc(n * sizeof(double));
  double* hmean = (double*)malloc(n * sizeof(double));
  double t_start = 0;
  double t_end = 0;
  int flag = 0;

  if (data==NULL||hdata==NULL||symmat==NULL||hsymmat==NULL||mean==NULL||hmean==NULL||stddev==NULL||hstddev==NULL){
    /* Something went wrong in the memory allocation here, fail gracefully */
    return(-10000);
  }

  for (i = 0; i < n; i++){ /*<<<<< 912, 1, 917, 4, loop48outer*/
    for (j = 0; j < n; j++){ /*<<<<< 913, 1, 916, 6, loop48inner*/
      data[i*n+j] =  i % 12 + 2 * (j % 7);
      hdata[i*n+j] = data[i*n+j];
    }
  }

  /* Determine mean of column vectors of input data matrix */
  for (j = 0; j < n; j++){ /*<<<<< 920, 1, 926, 4, loop49outer*/
    hmean[j] = 0.0;
    for (i = 0; i < n; i++){ /*<<<<< 922, 1, 924, 6, loop49inner*/
      hmean[j] += hdata[i*n+j];
    }
    hmean[j] /= n;
  }

  /* Determine standard deviations of column vectors of data matrix. */
  for (j = 0; j < n; j++){ /*<<<<< 929, 1, 940, 4, loop50outer*/
    hstddev[j] = 0.0;
    for (i = 0; i < n; i++){ /*<<<<< 931, 1, 933, 6, loop50inner*/
      hstddev[j] += (hdata[i*n+j] - hmean[j]) * (hdata[i*n+j] - hmean[j]);
    }
    hstddev[j] /= n-1; /* Unbiased estimator */
    hstddev[j] = sqrt(hstddev[j]);
    /* The following in an inelegant but usual way to handle
       near-zero std. dev. values, which below would cause a zero-
       divide. */
    hstddev[j] = hstddev[j] <= eps ? 1.0 : hstddev[j];
  }


  /* Center and reduce the column vectors. */
  for (i = 0; i < n; i++){ /*<<<<< 944, 1, 949, 4, loop51outer*/
    for (j = 0; j < n; j++){ /*<<<<< 945, 1, 948, 6, loop51outer*/
      hdata[i*n+j] -= hmean[j];
      hdata[i*n+j] = hdata[i*n+j] / hstddev[j];
    }
  }




  /* Calculate the n * n correlation matrix. */
  for (j1 = 0; j1 < n; j1++){ /*<<<<< 955, 1, 964, 4, loop52outer*/
    for (j2 = j1; j2 < n; j2++){ /*<<<<< 956, 1, 963, 6, loop52inner*/
      hsymmat[j1*n+j2] = 0.0;
      for (i = 0; i < n; i++){ /*<<<<< 958, 1, 960, 8, loop52inner2*/
        hsymmat[j1*n+j2] += hdata[i*n+j1] * hdata[i*n+j2];
      }
      hsymmat[j1*n+j2] /= n-1;
      hsymmat[j2*n+j1] = hsymmat[j1*n+j2];
    }
  }



  /* Accelerator */


  t_start = gettime();
  /* Determine mean of column vectors of input data matrix */
#pragma acc data copyin(data[0:n*n]), create(mean[0:n], stddev[0:n]), copyout(symmat[0:n*n])
  {

#pragma acc parallel loop private(tmp)
    for (j = 0; j < n; j++){ /*<<<<< 977, 1, 984, 6, loop53outer*/
      tmp = 0.0;
#pragma acc loop reduction(+:tmp)
      for (i = 0; i < n; i++){ /*<<<<< 980, 1, 982, 8, loop53inner*/
        tmp += data[i*n+j];
      }
      mean[j] = tmp / n;
    }

#pragma acc parallel loop private(tmp)
    for (j = 0; j < n; j++){ /*<<<<< 987, 1, 1002, 6, loop54outer*/
      stddev[j] = 0.0;
      tmp = 0;
#pragma acc loop reduction(+:tmp)
      for (i = 0; i < n; i++){ /*<<<<< 991, 1, 993, 8, loop54inner*/
        tmp += (data[i*n+j] - mean[j]) * (data[i*n+j] - mean[j]);
      }
      tmp /= (n-1);
      stddev[j] = sqrt(tmp);
      /*
       * The following in an inelegant but usual way to handle
       *   near-zero std. dev. values, which below would cause a zero-
       *  divide.
       */
      stddev[j] = stddev[j] <= eps ? 1.0 : stddev[j];
    }


#pragma acc parallel loop
    for (j = 0; j < n; j++){ /*<<<<< 1006, 1, 1012, 6, loop55outer*/
#pragma acc loop
      for (i = 0; i < n; i++){ /*<<<<< 1008, 1, 1011, 8, loop55inner*/
        data[i*n+j] -= mean[j];
        data[i*n+j] /= stddev[j];
      }
    }

#pragma acc parallel loop
    for (j1 = 0; j1 < n; j1++){ /*<<<<< 1015, 1, 1027, 6, loop56outer*/
      symmat[j1*n+j1] = 0;
#pragma acc loop private(tmp)
      for (j2 = j1; j2 < n; j2++){ /*<<<<< 1018, 1, 1026, 8, loop56inner*/
        tmp = 0;
#pragma acc loop reduction(+:tmp)
        for (i = 0; i < n; i++){ /*<<<<< 1021, 1, 1023, 10, loop56inner2*/
          tmp += (data[i*n+j1] * data[i*n+j2]);
        }
        symmat[j1*n+j2] = tmp/(n-1);
        symmat[j2*n+j1] = symmat[j1*n+j2];
      }
    }

  } /* end_data */
  t_end = gettime();


  for (i = 0; i < n;i++){ /*<<<<< 1033, 1, 1039, 4, loop57outer*/
    for (j = 0; j < n; j++){ /*<<<<< 1034, 1, 1038, 6, loop57inner*/
      if (fabs(symmat[i*n+j] - hsymmat[i*n+j])/hsymmat[i*n+j] > TOL) {
	flag = 1;
      }
    }
  }
  
  /* Free malloc'd memory to prevent leaks */
  free(data);
  free(hdata);
  free(symmat);
  free(hsymmat);
  free(mean);
  free(hmean);
  free(stddev);
  free(hstddev);

  if (flag==0) return (t_end-t_start);
  else return(-11000);
}


double syr2k(){

  double t_start = 0;
  double t_end = 0;
  extern unsigned int datasize;
  int n = 512;//sqrt((datasize/sizeof(double))/4);
  int i = 0;
  int j = 0;
  int k = 0;
  double alpha = 0;
  double beta = 0;
  double tmp = 0;
  int flag = 0;

  /* Array declaration */
  double* A = (double*)malloc(n*n * sizeof(double));
  double* B = (double*)malloc(n*n * sizeof(double));
  double* C = (double*)malloc(n*n * sizeof(double));
  double* CG = (double*)malloc(n*n * sizeof(double));

  if(A==NULL||B==NULL||C==NULL||CG==NULL){
    /* Something went wrong in the memory allocation here, fail gracefully */
    return(-10000);
  }


  alpha = 12435;
  beta = 4546;

  /* Init */
  for (i = 0; i < n; i++){ /*<<<<< 1086, 1, 1093, 4, loop58outer*/
    for (j = 0; j < n; j++){ /*<<<<< 1087, 1, 1092, 6, loop58inner*/
      C[i*n+j] = rand() / (1.0 + RAND_MAX);
      CG[i*n+j] = C[i*n+j];
      A[i*n+j] = rand() / (1.0 + RAND_MAX);
      B[i*n+j] = rand() / (1.0 + RAND_MAX);
    }
  }

  /* HOST */
  for (i = 0; i < n; i++){ /*<<<<< 1096, 1, 1100, 4, loop59outer*/
    for (j = 0; j < n; j++){ /*<<<<< 1097, 1, 1099, 6, loop59inner*/
      C[i*n+j] *= beta;
    }
  }

  for (i = 0; i < n; i++){ /*<<<<< 1102, 1, 1110, 4, loop60outer*/
    for (j = 0; j < n; j++){ /*<<<<< 1103, 1, 1109, 6, loop60inner*/
      tmp = C[i*n+j];
      for (k = 0; k < n; k++){ /*<<<<< 1105, 1, 1107, 8, loop60inner2*/
        tmp += (alpha * A[i*n+k] * B[j*n+k]) + (alpha * B[i*n+k] * A[j*n+k]);
      }
      C[i*n+j] = tmp;
    }
  }

  t_start = gettime();

  /* ACCELERATOR */
#pragma acc data copy(CG[0:n*n]), copyin(A[0:n*n], B[0:n*n])
  {
#pragma acc parallel loop
    for (i = 0; i < n; i++){ /*<<<<< 1118, 1, 1122, 6, loop61outer*/
      for (j = 0; j < n; j++){ /*<<<<< 1119, 1, 1121, 8, loop61inner*/
        CG[i*n+j] *= beta;
      }
    }

#pragma acc parallel loop
    for (i = 0; i < n; i++){ /*<<<<< 1125, 1, 1135, 6, loop62outer*/
#pragma acc loop private(tmp)
      for (j = 0; j < n; j++){ /*<<<<< 1127, 1, 1134, 8, loop62inner*/
        tmp = CG[i*n+j];
#pragma acc loop reduction(+:tmp)
        for (k = 0; k < n; k++){ /*<<<<< 1130, 1, 1132, 10, loop62inner2*/
          tmp += (alpha * A[i*n+k] * B[j*n+k]) + (alpha * B[i*n+k] * A[j*n+k]);
        }
        CG[i*n+j] = tmp;
      }
    }


  } /* end data */
  t_end = gettime();


  /* Compare */
  for (i = 0; i < n; i++){ /*<<<<< 1143, 1, 1149, 4, loop63outer*/
    for (j = 0; j < n; j++){ /*<<<<< 1144, 1, 1148, 6, loop63inner*/
      if (fabs(C[i*n+j] - CG[i*n+j])/C[i*n+j] > TOL){
        flag = 1;
      }
    }
  }

  /* Free malloc'd memory to prevent leaks */
  free(A);
  free(B);
  free(C);
  free(CG);

  if (flag==0) return (t_end-t_start);
  else return(-11000);
}


double gesummv(){

  int i = 0;
  int j = 0;
  int n = 4096;//sqrt((datasize/sizeof(double))/2);
  double alpha = 0;
  double beta = 0;
  double t1 = 0;
  double t2 = 0;
  double t_start= 0;
  double t_end = 0;
  int flag = 0;
  double* A = (double*)malloc(n*n * sizeof(double));
  double* B = (double*)malloc(n*n * sizeof(double));
  double* x = (double*)malloc(n * sizeof(double));
  double* Hy = (double*)malloc(n * sizeof(double));
  double* Ay = (double*)malloc(n * sizeof(double));
  double* Htmp = (double*)malloc(n * sizeof(double));

  if (A==NULL||B==NULL||x==NULL||Hy==NULL||Ay==NULL||Htmp==NULL){
    /* Something went wrong in the memory allocation here, fail gracefully */
    return(-10000);
  }


  alpha = 43532;
  beta = 12313;
  for (i = 0; i < n; i++){ /*<<<<< 1143, 1, 1149, 4, loop63outer*/
    x[i] = i / n;
    for (j = 0; j < n; j++){ /*<<<<< 1143, 1, 1149, 4, loop63outer*/
      A[i*n+j] = (i*j) / n;
      B[i*n+j] = A[i*n+j];
    }
  }

  /* Host */
  for (i = 0; i < n; i++){ /*<<<<< 1143, 1, 1149, 4, loop63outer*/
    Htmp[i] = 0;
    Hy[i] = 0;
    for (j = 0; j < n; j++){ /*<<<<< 1143, 1, 1149, 4, loop63outer*/
      Htmp[i] = A[i*n+j] * x[j] + Htmp[i];
      Hy[i] = B[i*n+j] * x[j] + Hy[i];
    }
    Hy[i] = alpha * Htmp[i] + beta * Hy[i];
  }




  /* Accelerator */
  t_start = gettime();
#pragma acc data copyin(A[0:n*n], B[0:n*n], x[0:n], beta, alpha), copyout(Ay[0:n]), create(i,j,t1,t2)
  {
#pragma acc parallel loop private(t1,t2)
    for (i = 0; i < n; i++){ /*<<<<< 1216, 1, 1225, 6, loop64outer*/
      t1 = 0;
      t2 = 0;
#pragma acc loop reduction(+:t1,t2)
      for (j = 0; j < n; j++){ /*<<<<< 1220, 1, 1223, 8, loop64inner*/
        t1 += A[i*n+j] * x[j];
	t2 += B[i*n+j] * x[j];
      }
      Ay[i] = alpha * t1 + beta * t2;
    }

  } /* end data */
  t_end = gettime();

  /* Compare */
  for (i=0;i<n;i++){ /*<<<<< 1231, 1, 1235, 4, loop65outer*/
    if ( fabs(Ay[i] - Hy[i])/Hy[i] > TOL ){
      flag = 1;
    }
  }

  /* Free malloc'd memory to prevent leaks */
  free(A);
  free(B);
  free(x);
  free(Ay);
  free(Hy);
  free(Htmp);

  if (flag==0) return (t_end-t_start);
  else return(-11000);
}

double gemm(){

  extern unsigned int datasize;
  int i = 0;
  int j = 0;
  int k = 0;
  double alpha = 0;
  double beta = 0;
  double* A = NULL;
  double* B = NULL;
  double* C = NULL;
  double* H = NULL;
  double t_start = 0;
  double t_end = 0;
  int flag = 0;
  int n = 0;
  n = 512;//sqrt((datasize/sizeof(double))/3);
  A = (double*)malloc(n*n * sizeof(double));
  B = (double*)malloc(n*n * sizeof(double));
  C = (double*)malloc(n*n * sizeof(double));
  H = (double*)malloc(n*n * sizeof(double));



  if (C==NULL||A==NULL||B==NULL||H==NULL){
    /* Something went wrong in the memory allocation here, fail gracefully */
    return(-10000);
  }

  alpha = 32412;
  beta = 2123;
  for (i = 0; i < n; i++){ /*<<<<< 1280, 1, 1287, 4, loop66outer*/
    for (j = 0; j < n; j++){ /*<<<<< 1281, 1, 1286, 6, loop66inner*/
      A[i*n+j] = rand()/(1.0+RAND_MAX);
      B[i*n+j] = rand()/(1.0+RAND_MAX);
      C[i*n+j] = rand()/(1.0+RAND_MAX);
      H[i*n+j] = C[i*n+j];
    }
  }


  /* Host for reference */
  for (i = 0; i < n; i++){ /*<<<<< 1291, 1, 1297, 4, loop67outer*/
    for (j = 0; j < n; j++){ /*<<<<< 1292, 1, 1196, 6, loop67inner*/
        H[i*n+j] *= beta;
        for (k = 0; k < n; k++) /*<<<<< 1294, 1, 1295, 51, loop67inner2*/
          H[i*n+j] += alpha * A[i*n+k] * B[k*n+j];
    }
  }



  /* Accelerator */
  t_start = gettime();
#pragma acc data copyin(A[0:n*n],B[0:n*n]), copy(C[0:n*n])
  {
#pragma acc kernels
#pragma acc loop independent
    for (i = 0; i < n; i++){ /*<<<<< 1307, 1, 1315, 6, loop68outer*/
#pragma acc loop independent
      for (j = 0; j < n; j++){ /*<<<<< 1309, 1, 1314, 8, loop68inner*/
        C[i*n+j] *= beta;
        for (k = 0; k < n; k++){ /*<<<<< 1311, 1, 1313, 10, loop68inner2*/
          C[i*n+j] += alpha * A[i*n+k] * B[k*n+j];
        }
      }
    }
  } /* end_data */

  t_end = gettime();

  /* Compare */
  for (i = 0; i < n; i++){ /*<<<<< 1321, 1, 1327, 4, loop69outer*/
    for (j = 0; j < n; j++){ /*<<<<< 1322, 1, 1326, 6, loop69inner*/
      if (fabs(H[i*n+j] - C[i*n+j])/H[i*n+j] > TOL){
        flag = 1;
      }
    }
  }

  /* Free malloc'd memory to prevent leaks */
  free(C);
  free(H);
  free(A);
  free(B);


  if (flag==0) return (t_end-t_start);
  else return(-11000);
}


double twodconv(){

  int i = 0;
  int j = 0;
  double c11, c12, c13, c21, c22, c23, c31, c32, c33;

  double t_start = 0;
  double t_end = 0;
  extern unsigned int datasize;
  int n = 4096;//sqrt((datasize/sizeof(double))/2);
  int flag = 0;

  double* A = (double*)malloc(n*n * sizeof(double));
  double* B = (double*)malloc(n*n * sizeof(double));
  double* B_Gpu = (double*)malloc(n*n * sizeof(double));

  if (A==NULL||B==NULL||B_Gpu==NULL){
    /* Something went wrong in the memory allocation here, fail gracefully */
    return(-10000);
  }

  for (i = 0; i < n; i++){ /*<<<<< 1362, 1, 1366, 4, loop70outer*/
    for (j = 0; j < n; j++){ /*<<<<< 1363, 1, 1365, 6, loop70inner*/
      A[i*n+j] = rand()/(1.0+RAND_MAX);
    }
  }



  c11 = +2;  c21 = +5;  c31 = -8;
  c12 = -3;  c22 = +6;  c32 = -9;
  c13 = +4;  c23 = +7;  c33 = +10;

  for (i = 1; i < n - 1; i++){ /*<<<<< 1374, 1, 1380, 4, loop71outer*/
    for (j = 1; j < n - 1; j++){ /*<<<<< 1375, 1, 1379, 6, loop71inner*/
      B[i*n+j] = c11 * A[(i-1)*n+(j-1)] + c12 * A[i*n+(j-1)] + c13 * A[(i+1)*n+(j-1)]
        + c21 * A[(i-1)*n+j] + c22 * A[i*n+j] + c23 * A[(i+1)*n+j]
        + c31 * A[(i-1)*n+(j+1)] + c32 * A[i*n+(j+1)] + c33 * A[(i+1)*n+(j+1)];
    }
  }



  t_start = gettime();
#pragma acc data copyin(A[0:n*n]), copyout(B_Gpu[0:n*n])
  {
#pragma acc kernels
#pragma acc loop independent
    for (i = 1; i < n - 1; i++){ /*<<<<< 1389, 1, 1395, 6, loop72outer*/
      for (j = 1; j < n - 1; j++){ /*<<<<< 1390, 1, 1394, 8, loop72inner*/
      B_Gpu[i*n+j] = c11 * A[(i-1)*n+(j-1)] + c12 * A[i*n+(j-1)] + c13 * A[(i+1)*n+(j-1)]
        + c21 * A[(i-1)*n+j] + c22 * A[i*n+j] + c23 * A[(i+1)*n+j]
        + c31 * A[(i-1)*n+(j+1)] + c32 * A[i*n+(j+1)] + c33 * A[(i+1)*n+(j+1)];
      }
    }
  }
  t_end = gettime();

  for (i = 1; i < n-1; i++){ /*<<<<< 1399, 1, 1405, 4, loop73outer*/
    for (j = 1; j < n-1; j++){ /*<<<<< 1400, 1, 1404, 6, loop73inner*/
      if (fabs(B[i*n+j] - B_Gpu[i*n+j])/B[i*n+j] > TOL){
        flag = 1;
      }
    }
  }
  /* Free malloc'd memory to prevent leaks */
  free(A);
  free(B);
  free(B_Gpu);


  if (flag==0) return (t_end-t_start);
  else return(-11000);

}


double threedconv(){

  int i, j, k;
  int n = 256;//cbrt((datasize/sizeof(double))/2);
  double c11, c12, c13, c21, c22, c23, c31, c32, c33;
  int flag = 0;
  double t_start, t_end;
  double* A = (double*)malloc(n*n*n * sizeof(double));
  double* B = (double*)malloc(n*n*n * sizeof(double));
  double* B_Gpu = (double*)malloc(n*n*n * sizeof(double));


  if (A==NULL||B==NULL||B_Gpu==NULL){
    /* Something went wrong in the memory allocation here, fail gracefully */
    return(-10000);
  }

  c11 = +2;  c21 = +5;  c31 = -8;
  c12 = -3;  c22 = +6;  c32 = -9;
  c13 = +4;  c23 = +7;  c33 = +10;


  /* Init */
  for (i = 0; i < n; i++){ /*<<<<< 1441, 1, 1447, 4, loop74outer*/
    for (j = 0; j < n; j++){ /*<<<<< 1442, 1, 1446, 6, loop74inner*/
      for (k = 0; k < n; k++){ /*<<<<< 1443, 1, 1445, 8, loop74inner2*/
        A[i*n*n+j*n+k] = rand()/(1.0+RAND_MAX);
      }
    }
  }

  /* HOST */
  for (i = 1; i < n - 1; i++){ /*<<<<< 1450, 1, 1465, 4, loop75outer*/
    for (j = 1; j < n - 1; j++){ /*<<<<< 1451, 1, 1464, 6, loop75inner*/
      for (k = 1; k < n - 1; k++){ /*<<<<< 1452, 1, 1463, 8, loop75inner2*/
        B[i*n*n+j*n+k] = 0
          +   c11 * A[(i-1)*n*n + (j-1)*n + (k-1)]  +  c13 * A[(i+1)*n*n + (j-1)*n + (k-1)]
          +   c21 * A[(i-1)*n*n + (j-1)*n + (k-1)]  +  c23 * A[(i+1)*n*n + (j-1)*n + (k-1)]
          +   c31 * A[(i-1)*n*n + (j-1)*n + (k-1)]  +  c33 * A[(i+1)*n*n + (j-1)*n + (k-1)]
          +   c12 * A[(i+0)*n*n + (j-1)*n + (k+0)]
          +   c22 * A[(i+0)*n*n + (j+0)*n + (k+0)]
          +   c32 * A[(i+0)*n*n + (j+1)*n + (k+0)]
          +   c11 * A[(i-1)*n*n + (j-1)*n + (k+1)]  +  c13 * A[(i+1)*n*n + (j-1)*n + (k+1)]
	  +   c21 * A[(i-1)*n*n + (j+0)*n + (k+1)]  +  c23 * A[(i+1)*n*n + (j+0)*n + (k+1)]
	  +   c31 * A[(i-1)*n*n + (j+1)*n + (k+1)]  +  c33 * A[(i+1)*n*n + (j+1)*n + (k+1)];
      }
    }
  }



  /* ACCELERATOR */
  t_start = gettime();
#pragma acc data copyin(A[0:n*n*n]), copyout(B_Gpu[0:n*n*n])
  {
#pragma acc kernels
#pragma acc loop independent
    for (i = 1; i < n - 1; i++){ /*<<<<< 1475, 1, 1490, 6, loop76outer*/
      for (j = 1; j < n - 1; j++){ /*<<<<< 1476, 1, 1489, 6, loop76inner*/
        for (k = 1; k < n - 1; k++){ /*<<<<< 1477, 1, 1488, 10, loop76inner2*/
          B_Gpu[i*n*n+j*n+k] = 0
          +   c11 * A[(i-1)*n*n + (j-1)*n + (k-1)]  +  c13 * A[(i+1)*n*n + (j-1)*n + (k-1)]
          +   c21 * A[(i-1)*n*n + (j-1)*n + (k-1)]  +  c23 * A[(i+1)*n*n + (j-1)*n + (k-1)]
          +   c31 * A[(i-1)*n*n + (j-1)*n + (k-1)]  +  c33 * A[(i+1)*n*n + (j-1)*n + (k-1)]
          +   c12 * A[(i+0)*n*n + (j-1)*n + (k+0)]
          +   c22 * A[(i+0)*n*n + (j+0)*n + (k+0)]
          +   c32 * A[(i+0)*n*n + (j+1)*n + (k+0)]
          +   c11 * A[(i-1)*n*n + (j-1)*n + (k+1)]  +  c13 * A[(i+1)*n*n + (j-1)*n + (k+1)]
	  +   c21 * A[(i-1)*n*n + (j+0)*n + (k+1)]  +  c23 * A[(i+1)*n*n + (j+0)*n + (k+1)]
	  +   c31 * A[(i-1)*n*n + (j+1)*n + (k+1)]  +  c33 * A[(i+1)*n*n + (j+1)*n + (k+1)];
        }
      }
    }

  }
  t_end = gettime();

  /* Compare */
  for (i = 1; i < n - 1; i++){ /*<<<<< 1496, 1, 1504, 4, loop77outer*/
    for (j = 1; j < n - 1; j++){ /*<<<<< 1497, 1, 1503, 6, loop77inner*/
      for (k = 1; k < n - 1; k++){ /*<<<<< 1498, 1, 1502, 8, loop77inner2*/
        if (fabs(B[i*n*n+j*n+k] - B_Gpu[i*n*n+j*n+k])/B[i*n*n+j*n+k] > TOL){
          flag = 1;
        }
      }
    }
  }


  /* Free malloc'd memory to prevent leaks */
  free(A);
  free(B);
  free(B_Gpu);

  if (flag==0) return (t_end-t_start);
  else return(-11000);
}
