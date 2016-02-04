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

  for (i = 0; i < n; i++){ /* loop1outer */
    for (j = 0; j < n; j++){ /* loop1inner */
      A[i*n+j] = rand()/ (1.0 + RAND_MAX);
    }
  }
  for (i = 0; i < n; i++){ /* loop2outer */
    for (j = 0; j < n; j++){ /* loop2inner */
      B[i*n+j] = rand()/ (1.0 + RAND_MAX);
    }
  }
  for (i = 0; i < n; i++){ /* loop3outer */
    for (j = 0; j < n; j++){ /* loop3inner */
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
    for (i = 0; i < n; i++){ /* loop14outer */
#pragma acc loop private(tmp)
      for (j = 0; j < n; j++){ /* loop4inner */
	tmp = 0;
#pragma acc loop reduction(+:tmp)
        for (k = 0; k < n; k++){ /* loop4inner2 */
	  tmp += A[i*n+k] * B[k*n+j];
        }
	C[i*n+j] = tmp;
      }
    }


#pragma acc parallel loop private(tmp)
    for (i = 0; i < n; i++){ /* loop5outer */
#pragma acc loop private(tmp)
      for (j = 0; j < n; j++){ /* loop5inner */
	tmp = 0;
#pragma acc loop reduction(+:tmp)
        for (k = 0; k < n; k++){ /* loop5inner2 */
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


  for (i = 0; i < n; i++){ /* loop6outer */
    for (j = 0; j < n; j++){ /* loop6inner */
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
  /* loop7outer */
  for (i = 0; i < n; i++) {
	/* loop7inner */
    for (j = 0; j < n; j++){
      E[i*n+j] = 0;
      /* loop7inner2 */
      for (k = 0; k < n; k++){
        E[i*n+j] += A[i*n+k] * B[k*n+j];
      }
    }
  }

  /* F := C*D */
  /* loop8outer */
  for (i = 0; i < n; i++) {
	  /* loop8inner */
    for (j = 0; j < n; j++) {
      F[i*n+j] = 0;
      /* loop8inner2 */
      for (k = 0; k < n; k++){
        F[i*n+j] += C[i*n+k] * D[k*n+j];
      }
    }
  }

  /* G := E*F */
  /* loop9outer */
  for (i = 0; i < n; i++) {
	  /* loop9inner */
    for (j = 0; j < n; j++) {
      H[i*n+j] = 0;
      /* loop9inner2 */
      for (k = 0; k < n; k++){
        H[i*n+j] += E[i*n+k] * F[k*n+j];
      }
    }
  }



  t_start = gettime();
#pragma acc data copyin(A[0:n*n],B[0:n*n],C[0:n*n],D[0:n*n]), create(E[0:n*n],F[0:n*n]), copyout(G[0:n*n])
  {
#pragma acc parallel loop
	  /* loop10outer */
    for (i = 0; i < n; i++) {
#pragma acc loop
    	/* loop10inner */
      for (j = 0; j < n; j++){
        E[i*n+j] = 0;
        /* loop10inner2 */
        for (k = 0; k < n; k++){
          E[i*n+j] += A[i*n+k] * B[k*n+j];
        }
      }
    }

    /* F := C*D */
#pragma acc parallel loop
    /* loop11outer */
    for (i = 0; i < n; i++) {
#pragma acc loop
    	/* loop11inner */
      for (j = 0; j < n; j++) {
        F[i*n+j] = 0;
        /* loop11inner2 */
        for (k = 0; k < n; k++){
          F[i*n+j] += C[i*n+k] * D[k*n+j];
        }
      }
    }

    /* G := E*F */
#pragma acc parallel loop
    /* loop12outer */
    for (i = 0; i < n; i++) {
#pragma acc loop
    	/* loop12inner */
      for (j = 0; j < n; j++) {
        G[i*n+j] = 0;
        /* loop12inner2 */
        for (k = 0; k < n; k++){
          G[i*n+j] += E[i*n+k] * F[k*n+j];
        }
      }
    }

  } /* end data */
  t_end=gettime();


  /* loop13outer */
  for (i = 0; i < n; i++){
	  /* loop13inner */
    for (j = 0; j < n; j++){
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


  /* loop14outer */
  for (i = 0; i < n; i++){
    x[i] = rand() / (1.0 + RAND_MAX);
    y[i] = 0;
    /* loop14inner */
    for (j = 0; j < n; j++){
      A[i*n+j] = rand() / (1.0 + RAND_MAX);
    }
  }

  /* loop15outer */
  for (i = 0; i < n; i++){
    tmpscalar = 0;
    /* loop15inner */
    for (j = 0; j < n; j++){
      tmpscalar += A[i*n+j] * x[j];
    }
    tmp[i] = tmpscalar;
  }

  /* loop16outer */
  for (j = 0; j < n; j++){
    tmpscalar = 0;
    /* loop16inner */
    for (i = 0; i < n; i++){
      tmpscalar += A[i*n+j] * tmp[i];
    }
    y[j] = tmpscalar;
  }

  tmpscalar = 0;

  t_start = gettime();
#pragma acc data copyin(A[0:n*n], x[0:n]), create(tmp[0:n]), copyout(ya[0:n])
  {

#pragma acc parallel loop private(tmpscalar)
	  /* loop17outer */
    for (i = 0; i < n; i++){
      tmpscalar = 0;
#pragma acc loop reduction(+:tmpscalar)
      /* loop17inner */
      for (j = 0; j < n; j++){
        tmpscalar += A[i*n+j] * x[j];
      }
      tmp[i] = tmpscalar;
    }

#pragma acc parallel loop private(tmpscalar)
    /* loop18outer */
    for (j = 0; j < n; j++){
      tmpscalar = 0;
#pragma acc loop reduction(+:tmpscalar)
      /* loop18inner */
      for (i = 0; i < n; i++){
        tmpscalar += A[i*n+j] * tmp[i];
      }
      ya[j] = tmpscalar;

    }

  } /* end data */
  t_end = gettime();
  /* loop19outer */
  for (j = 0; j < n; j++){
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

  /* loop20outer */
  for (i = 0; i < n; i++) {
    r[i] = rand() / (1.0 + RAND_MAX);
    p[i] = rand() / (1.0 + RAND_MAX);
    /* loop20inner */
    for (j = 0; j < n; j++) {
      A[i*n+j] = rand() / (1.0 + RAND_MAX);
    }
  }


  t_start = gettime();
#pragma acc data copyin(A[0:n*n],r[0:n],p[0:n]), copyout(q[0:n],s[0:n])
  {

#pragma acc parallel loop private(tmp)
	  /* loop21outer */
    for (i = 0; i < n; i++){
      tmp = 0;
#pragma acc loop reduction(+:tmp)
      /* loop21inner */
      for (j = 0; j < n; j++){
        tmp += A[i*n+j] * p[j];
      }
      q[i] = tmp;
    }

#pragma acc parallel loop private(tmp)
    /* loop22outer */
    for (j = 0; j < n; j++){
      tmp = 0;
#pragma acc loop reduction(+:tmp)
      /* loop22inner */
      for (i = 0; i < n; i++){
        tmp += r[i] * A[i*n+j];
      }
      s[j] = tmp;
    }


  }
  t_end = gettime();


  /* loop23outer */
  for (i = 0; i < n; i++){
    tmp = 0;
    /* loop23inner */
    for (j = 0; j < n; j++){
      tmp += A[i*n+j] * p[j];
    }
    qh[i] = tmp;
  }


  /* loop24outer */
  for (j = 0; j < n; j++){
    tmp = 0;
    /* loop24inner */
    for (i = 0; i < n; i++){
      tmp += r[i] * A[i*n+j];
    }
    sh[j] = tmp;
  }

  /* loop25outer */
  for (j = 0; j < n; j++){
    if (fabs(qh[j] - q[j])/qh[j] > TOL ){
      flag = 1;
    }
  }

  /* loop26outer */
  for (j = 0; j < n; j++){
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
  /* loop27outer */
  for (i = 0; i < n; i++){
    x1[i] = (i+1) / n;
    x1_Gpu[i] = (i+1) / n;
    x2[i] = (i + 1) / n;
    x2_Gpu[i] = (i + 1) / n;
    y1[i] = (i + 3) / n;
    y2[i] = (i + 4) / n;
    for (j = 0; j < n; j++){ /* loop27inner */
      a[i*n+j] = ((i+1)*(j+1)) / n;
    }
  }

  /* HOST */
  /* loop28outer */
  for (i=0; i<n; i++){
    tmp1 = 0;
    /* loop28inner */
    for (j=0; j<n; j++){
      tmp1 += a[i*n+j] * y1[j];
    }
    x1[i] += tmp1;
  }


  /* loop29outer */
  for (i=0; i<n; i++){
    tmp2 = 0;
    /* loop29inner */
    for (j=0; j<n; j++){
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
	  /* loop30outer */
    for (i=0; i<n; i++){
      tmp1 = 0;
#pragma acc loop reduction(+:tmp1)
      /* loop30inner */
      for (j=0; j<n; j++){
        tmp1 += a[i*n+j] * y1[j];
      }
      x1_Gpu[i] += tmp1;
    }


#pragma acc parallel loop private(tmp2)
    /* loop31outer */
    for (i=0; i<n; i++){
      tmp2 = 0;
#pragma acc loop reduction(+:tmp2)
      /* loop31inner */
      for (j=0; j<n; j++){
        tmp2 += a[j*n+i] * y2[j];
      }
      x2_Gpu[i] += tmp2;
    }


  } /* end data */
  t_end = gettime();

  /* Compare */
  /* loop32outer */
  for (i=0; i<n; i++){
    if (fabs(x1[i] - x1_Gpu[i])/x1[i] > TOL){
      flag1 = 1;
    }
  }

  /* loop33outer */
  for (i=0; i<n; i++){
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
  /* loop34outer */
  for (i = 0; i < n; i++){
	  /* loop34inner */
    for (j = 0; j < n; j++){
      A[i*n+j] = (i*j) / n;
      C[i*n+j] = (i*j + 2) / n;
      CG[i*n+j] = (i*j + 2) / n;
    }
  }

  /* HOST */
  /* loop35outer */
  for (i = 0; i < n; i++){
	  /* loop35inner */
    for (j = 0; j < n; j++){
      C[i*n+j] *= beta;
    }
  }

  /* loop36outer */
  for (i = 0; i < n; i++){
	  /* loop36inner */
    for (j = 0; j < n; j++){
    	/* loop36inner2 */
      for (k = 0; k < n; k++){
        C[i*n+j] += alpha * A[i*n+k] * A[j*n+k];
      }
    }
  }


  /* ACCEL */

  t_start = gettime();
#pragma acc data copyin(A[0:n*n]), copy(CG[0:n*n])
  {
#pragma acc parallel loop
	  /* loop37outer */
    for (i = 0; i < n; i++){
#pragma acc loop
    	/* loop37inner */
      for (j = 0; j < n; j++){
        CG[i*n+j] *= beta;
      }
    }

#pragma acc parallel loop
    /* loop38outer */
    for (i = 0; i < n; i++){
#pragma acc loop
    	/* loop38inner */
      for (j = 0; j < n; j++){
        tmp = CG[i*n+j];
        /* loop38inner2 */
        for (k = 0; k < n; k++){
          tmp += alpha * A[i*n+k] * A[j*n+k];
        }
        CG[i*n+j] = tmp;
      }
    }

  } /* end data */
  t_end = gettime();

  /* Compare */
  /* loop39outer */
  for (i = 0; i < n; i++){
	  /* loop39inner */
    for (j = 0; j < n; j++){
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

  /* loop40outer */
  for (i = 0; i < n; i++){
	  /* loop40inner */
    for (j = 0; j < n; j++){
      data[i*n+j] =  i % 12 + 2 * (j % 7);
      hdata[i*n+j] = data[i*n+j];
    }
  }



  /* Determine mean of column vectors of input data matrix */
  /* loop41outer */
  for (j = 0; j < n; j++){
    hmean[j] = 0.0;
    /* loop41inner */
    for (i = 0; i < n; i++){
      hmean[j] += hdata[i*n+j];
    }
    hmean[j] /= n;
  }

  /* Center the column vectors. */
  /* loop42outer */
  for (i = 0; i < n; i++){
	  /* loop42inner */
    for (j = 0; j < n; j++){
      hdata[i*n+j] -= hmean[j];
    }
  }

  /* Calculate the n * n covariance matrix. */
  /* loop43outer */
  for (j1 = 0; j1 < n; j1++){
	  /* loop43inner */
    for (j2 = j1; j2 < n; j2++){
      hsymmat[j1*n+j2] = 0.0;
      /* loop43inner2 */
      for (i = 0; i < n; i++){
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
	  /* loop44outer */
    for (j = 0; j < n; j++){
      tmp = 0;
#pragma acc loop reduction(+:tmp)
      /* loop44inner */
      for (i = 0; i < n; i++){
        tmp += data[i*n+j];
      }
      mean[j] = tmp/n;
    }

    /* Center the column vectors. */
#pragma acc parallel loop
    /* loop45outer */
    for (i = 0; i < n; i++){
#pragma acc loop
    	/* loop45inner */
      for (j = 0; j < n; j++){
        data[i*n+j] -= mean[j];
      }
    }

    /* Calculate the n * n covariance matrix. */
#pragma acc parallel loop
    /* loop46outer */
    for (j1 = 0; j1 < n; j1++){
#pragma acc loop private(tmp)
    	/* loop46inner */
      for (j2 = j1; j2 < n; j2++){
        tmp = 0;
#pragma acc loop reduction(+:tmp)
        /* loop46inner2 */
        for (i = 0; i < n; i++){
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
  /* loop47outer */
  for (j1 = 0; j1 < n; j1++){
	  /* loop47inner */
    for (j2 = 0; j2 < n; j2++){
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
  /* loop48outer */
  for (i = 0; i < n; i++){
	  /* loop48inner */
    for (j = 0; j < n; j++){
      data[i*n+j] =  i % 12 + 2 * (j % 7);
      hdata[i*n+j] = data[i*n+j];
    }
  }

  /* Determine mean of column vectors of input data matrix */
  /* loop49outer */
  for (j = 0; j < n; j++){
    hmean[j] = 0.0;
    /* loop49inner */
    for (i = 0; i < n; i++){
      hmean[j] += hdata[i*n+j];
    }
    hmean[j] /= n;
  }

  /* Determine standard deviations of column vectors of data matrix. */
  /* loop50outer */
  for (j = 0; j < n; j++){
    hstddev[j] = 0.0;
    /* loop50inner */
    for (i = 0; i < n; i++){
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
  /* loop51outer */
  for (i = 0; i < n; i++){
	  /* loop51outer */
    for (j = 0; j < n; j++){
      hdata[i*n+j] -= hmean[j];
      hdata[i*n+j] = hdata[i*n+j] / hstddev[j];
    }
  }




  /* Calculate the n * n correlation matrix. */
  /* loop52outer */
  for (j1 = 0; j1 < n; j1++){
	  /* loop52inner */
    for (j2 = j1; j2 < n; j2++){
      hsymmat[j1*n+j2] = 0.0;
      /* loop52inner2 */
      for (i = 0; i < n; i++){
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
	  /* loop53outer */
    for (j = 0; j < n; j++){
      tmp = 0.0;
#pragma acc loop reduction(+:tmp)
      /* loop53inner */
      for (i = 0; i < n; i++){
        tmp += data[i*n+j];
      }
      mean[j] = tmp / n;
    }

#pragma acc parallel loop private(tmp)
    /* loop54outer */
    for (j = 0; j < n; j++){
      stddev[j] = 0.0;
      tmp = 0;
#pragma acc loop reduction(+:tmp)
      /* loop54inner */
      for (i = 0; i < n; i++){
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
    /* loop55outer */
    for (j = 0; j < n; j++){
#pragma acc loop
    	/* loop55inner */
      for (i = 0; i < n; i++){
        data[i*n+j] -= mean[j];
        data[i*n+j] /= stddev[j];
      }
    }

#pragma acc parallel loop
    /* loop56outer */
    for (j1 = 0; j1 < n; j1++){
      symmat[j1*n+j1] = 0;
#pragma acc loop private(tmp)
      /* loop56inner */
      for (j2 = j1; j2 < n; j2++){
        tmp = 0;
#pragma acc loop reduction(+:tmp)
        /* loop56inner2 */
        for (i = 0; i < n; i++){
          tmp += (data[i*n+j1] * data[i*n+j2]);
        }
        symmat[j1*n+j2] = tmp/(n-1);
        symmat[j2*n+j1] = symmat[j1*n+j2];
      }
    }

  } /* end_data */
  t_end = gettime();


  /* loop57outer */
  for (i = 0; i < n;i++){
	  /* loop57inner */
    for (j = 0; j < n; j++){
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
  /* loop58outer */
  for (i = 0; i < n; i++){
	  /* loop58inner */
    for (j = 0; j < n; j++){
      C[i*n+j] = rand() / (1.0 + RAND_MAX);
      CG[i*n+j] = C[i*n+j];
      A[i*n+j] = rand() / (1.0 + RAND_MAX);
      B[i*n+j] = rand() / (1.0 + RAND_MAX);
    }
  }

  /* HOST */
  /* loop59outer */
  for (i = 0; i < n; i++){
	  /* loop59inner */
    for (j = 0; j < n; j++){
      C[i*n+j] *= beta;
    }
  }

  /* loop60outer */
  for (i = 0; i < n; i++){
	  /* loop60inner */
    for (j = 0; j < n; j++){
      tmp = C[i*n+j];
      /* loop60inner2 */
      for (k = 0; k < n; k++){
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
	  /* loop61outer */
    for (i = 0; i < n; i++){
    	/* loop61inner */
      for (j = 0; j < n; j++){
        CG[i*n+j] *= beta;
      }
    }

#pragma acc parallel loop
    /* loop62outer */
    for (i = 0; i < n; i++){
#pragma acc loop private(tmp)
    	/* loop62inner */
      for (j = 0; j < n; j++){
        tmp = CG[i*n+j];
#pragma acc loop reduction(+:tmp)
        /* loop62inner2 */
        for (k = 0; k < n; k++){
          tmp += (alpha * A[i*n+k] * B[j*n+k]) + (alpha * B[i*n+k] * A[j*n+k]);
        }
        CG[i*n+j] = tmp;
      }
    }


  } /* end data */
  t_end = gettime();


  /* Compare */
  /* loop63outer */
  for (i = 0; i < n; i++){
	  /* loop63inner */
    for (j = 0; j < n; j++){
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
  /* loop63outer */
  for (i = 0; i < n; i++){
    x[i] = i / n;
    /* loop63outer */
    for (j = 0; j < n; j++){
      A[i*n+j] = (i*j) / n;
      B[i*n+j] = A[i*n+j];
    }
  }

  /* Host */
  /* loop63outer */
  for (i = 0; i < n; i++){
    Htmp[i] = 0;
    Hy[i] = 0;
    /* loop63outer */
    for (j = 0; j < n; j++){
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
	  /* loop64outer */
    for (i = 0; i < n; i++){
      t1 = 0;
      t2 = 0;
#pragma acc loop reduction(+:t1,t2)
      /* loop64inner */
      for (j = 0; j < n; j++){
        t1 += A[i*n+j] * x[j];
	t2 += B[i*n+j] * x[j];
      }
      Ay[i] = alpha * t1 + beta * t2;
    }

  } /* end data */
  t_end = gettime();

  /* Compare */
  /* loop65outer */
  for (i=0;i<n;i++){
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
  /* loop66outer */
  for (i = 0; i < n; i++){
	  /* loop66inner */
    for (j = 0; j < n; j++){
      A[i*n+j] = rand()/(1.0+RAND_MAX);
      B[i*n+j] = rand()/(1.0+RAND_MAX);
      C[i*n+j] = rand()/(1.0+RAND_MAX);
      H[i*n+j] = C[i*n+j];
    }
  }


  /* Host for reference */
  /* loop67outer */
  for (i = 0; i < n; i++){
	  /* loop67inner */
    for (j = 0; j < n; j++){
        H[i*n+j] *= beta;
        /* loop67inner2 */
        for (k = 0; k < n; k++)
          H[i*n+j] += alpha * A[i*n+k] * B[k*n+j];
    }
  }



  /* Accelerator */
  t_start = gettime();
#pragma acc data copyin(A[0:n*n],B[0:n*n]), copy(C[0:n*n])
  {
#pragma acc kernels
#pragma acc loop independent
	  /* loop68outer */
    for (i = 0; i < n; i++){
#pragma acc loop independent
    	/* loop68inner */
      for (j = 0; j < n; j++){
        C[i*n+j] *= beta;
        /* loop68inner2 */
        for (k = 0; k < n; k++){
          C[i*n+j] += alpha * A[i*n+k] * B[k*n+j];
        }
      }
    }
  } /* end_data */

  t_end = gettime();

  /* Compare */
  /* loop69outer */
  for (i = 0; i < n; i++){
	  /* loop69inner */
    for (j = 0; j < n; j++){
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

  /* loop70outer */
  for (i = 0; i < n; i++){
	  /* loop70inner */
    for (j = 0; j < n; j++){
      A[i*n+j] = rand()/(1.0+RAND_MAX);
    }
  }



  c11 = +2;  c21 = +5;  c31 = -8;
  c12 = -3;  c22 = +6;  c32 = -9;
  c13 = +4;  c23 = +7;  c33 = +10;

  /* loop71outer */
  for (i = 1; i < n - 1; i++){
	  /* loop71inner */
    for (j = 1; j < n - 1; j++){
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
	  /* loop72outer */
    for (i = 1; i < n - 1; i++){
    	/* loop72inner */
      for (j = 1; j < n - 1; j++){
      B_Gpu[i*n+j] = c11 * A[(i-1)*n+(j-1)] + c12 * A[i*n+(j-1)] + c13 * A[(i+1)*n+(j-1)]
        + c21 * A[(i-1)*n+j] + c22 * A[i*n+j] + c23 * A[(i+1)*n+j]
        + c31 * A[(i-1)*n+(j+1)] + c32 * A[i*n+(j+1)] + c33 * A[(i+1)*n+(j+1)];
      }
    }
  }
  t_end = gettime();
  /* loop73outer */
  for (i = 1; i < n-1; i++){
	  /* loop73inner */
    for (j = 1; j < n-1; j++){
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
  /* loop74outer */
  for (i = 0; i < n; i++){
	  /* loop74inner */
    for (j = 0; j < n; j++){
    	/* loop74inner2 */
      for (k = 0; k < n; k++){
        A[i*n*n+j*n+k] = rand()/(1.0+RAND_MAX);
      }
    }
  }

  /* HOST */
  /* loop75outer */
  for (i = 1; i < n - 1; i++){
	  /* loop75inner */
    for (j = 1; j < n - 1; j++){
    	/* loop75inner2 */
      for (k = 1; k < n - 1; k++){
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
	  /* loop76outer */
    for (i = 1; i < n - 1; i++){
    	/* loop76inner */
      for (j = 1; j < n - 1; j++){
    	  /* loop76inner2 */
        for (k = 1; k < n - 1; k++){
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
  /* loop77outer */
  for (i = 1; i < n - 1; i++){
	  /* loop77inner */
    for (j = 1; j < n - 1; j++){
    	/* loop77inner2 */
      for (k = 1; k < n - 1; k++){
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
