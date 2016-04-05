/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jeff Overbey (Auburn) - initial API and implementation
 *******************************************************************************/
package edu.auburn.oaccrefac.internal.core.tests;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.eclipse.ptp.pldt.openacc.core.parser.OpenACCParser;
import org.eclipse.ptp.pldt.openacc.core.parser.SyntaxException;
import org.junit.Test;

public class OpenACCParserTests {

    String[] epccPragmas = { //
            "#pragma acc kernels loop independent, present(inGrid[0:nx*ny],outGrid[0:nx*ny])", //
            "#pragma acc data copy(CG[0:n*n]), copyin(A[0:n*n], B[0:n*n])", //
            "#pragma acc data copy(a0[0:sz*sz*sz]), create(a1[0:sz*sz*sz], i,j,k,iter), copyin(sz,fac,n)", //
            "#pragma acc data copy(arr_b[0:data]), copyin(arr_a[0:data])", //
            "#pragma acc data copy(gr[0:x*y], tgr[0:x*y])", //
            "#pragma acc data copyin(A[0:n*n*n]), copyout(B_Gpu[0:n*n*n])", //
            "#pragma acc data copyin(A[0:n*n]), copy(CG[0:n*n])", //
            "#pragma acc data copyin(A[0:n*n]), copyout(B_Gpu[0:n*n])", //
            "#pragma acc data copyin(A[0:n*n], B[0:n*n], x[0:n], beta, alpha), copyout(Ay[0:n]), create(i,j,t1,t2)", //
            "#pragma acc data copyin(A[0:n*n], x[0:n]), create(tmp[0:n]), copyout(ya[0:n])", //
            "#pragma acc data copyin(A[0:n*n],B[0:n*n]), copy(C[0:n*n])", //
            "#pragma acc data copyin(A[0:n*n],B[0:n*n],C[0:n*n],D[0:n*n]), create(E[0:n*n],F[0:n*n]), copyout(G[0:n*n])", //
            "#pragma acc data copyin(A[0:n*n],B[0:n*n],D[0:n*n], n), copyout(E[0:n*n]), create(C[0:n*n], tmp,i,j,k)", //
            "#pragma acc data copyin(A[0:n*n],r[0:n],p[0:n]), copyout(q[0:n],s[0:n])", //
            "#pragma acc data copyin(a,b,p), create(i,j,k,s0,ss)", //
            "#pragma acc data copyin(a[0:5])", //
            "#pragma acc data copyin(a[0:n*n], y2[0:n], y1[0:n]), copy(x1_Gpu[0:n], x2_Gpu[0:n])", //
            "#pragma acc data copyin(a[0:n])", //
            "#pragma acc data copyin(a[0:n],i,n)", //
            "#pragma acc data copyin(aptr[0:sqrtn])", //
            "#pragma acc data copyin(data[0:n*n]),  copyout(symmat[0:n*n]), create(mean[0:n])", //
            "#pragma acc data copyin(data[0:n*n]), create(mean[0:n], stddev[0:n]), copyout(symmat[0:n*n])", //
            "#pragma acc data copyout(a[0:n])", //
            "#pragma acc data copyout(aptr[0:sqrtn])", //
            "#pragma acc kernels", //
            "#pragma acc kernels create(a[0:n])", //
            "#pragma acc kernels if(0)", //
            "#pragma acc kernels loop", //
            "#pragma acc kernels loop create(a[0:n])", //
            "#pragma acc loop", //
            "#pragma acc loop independent", //
            "#pragma acc loop private(tmp)", //
            "#pragma acc loop reduction(+:t1,t2)", //
            "#pragma acc loop reduction(+:tmp)", //
            "#pragma acc loop reduction(+:tmp1)", //
            "#pragma acc loop reduction(+:tmp2)", //
            "#pragma acc loop reduction(+:tmpscalar)", //
            "#pragma acc loop reduction(+:z)", //
            "#pragma acc loop vector", //
            "#pragma acc parallel create(a[0:n])", //
            "#pragma acc parallel if(0)", //
            "#pragma acc parallel loop", //
            "#pragma acc parallel loop create(a[0:n])", //
            "#pragma acc parallel loop create(a[0:n]) copyin(z[0:n])", //
            "#pragma acc parallel loop create(a[0:n]), firstprivate(z[0:n])", //
            "#pragma acc parallel loop create(a[0:ppn]), private(z[0:ppn]), num_gangs(256)", //
            "#pragma acc parallel loop create(a[0:ppn],z[0:ppn]), num_gangs(256)", //
            "#pragma acc parallel loop gang, present(inGrid[0:nx*ny],outGrid[0:nx*ny])", //
            "#pragma acc parallel loop private(i,j,k,s0,ss), reduction(+:gosa)", //
            "#pragma acc parallel loop private(t1,t2)", //
            "#pragma acc parallel loop private(tmp)", //
            "#pragma acc parallel loop private(tmp1)", //
            "#pragma acc parallel loop private(tmp2)", //
            "#pragma acc parallel loop private(tmpscalar)", //
            "#pragma acc parallel loop reduction(+:z)", //
            "#pragma acc update host(a[0:n])", //
            "#pragma acc wait", //
            "#pragma acc wait      ", //
    };

    private String[] openaccExamplePragmas = { //
            "            #pragma acc host_data use_device(a,b,c)", //
            "        #pragma acc data copyin(a[0:rowa*cola],b[0:rowb*colb]) copy(c[0:rowc*colc])", //
            "        #pragma acc host_data use_device(data)", //
            "    #pragma acc data copy(data[0:2*n])" //
    };

    private String[] npbPragmas = { //
            "         #pragma acc loop worker vector reduction(+:sum)", //
            "       #pragma acc loop gang   ", //
            "       #pragma acc loop gang worker vector reduction(+:d) ", //
            "       #pragma acc loop gang worker vector reduction(+:rho)", //
            "       #pragma acc loop worker vector reduction(+:sum_qi)", //
            "       #pragma acc loop vector", //
            "      #pragma acc loop vector", //
            "      #pragma acc parallel loop gang num_gangs(isize-1) num_workers(8) vector_length(32)", //
            "     #pragma acc loop vector", //
            "     #pragma acc parallel loop gang num_gangs(isize+1) num_workers(8) vector_length(32)", //
            "     #pragma acc parallel loop gang num_gangs(jsize+1) num_workers(8) vector_length(32)", //
            "     #pragma acc parallel loop gang num_gangs(jsize-1) num_workers(4) vector_length(32)", //
            "     #pragma acc parallel loop gang num_gangs(ksize+1) num_workers(4) vector_length(32)", //
            "     #pragma acc parallel loop gang num_gangs(ksize-1) num_workers(4) vector_length(32)", //
            "   #pragma acc kernels loop gang((end+1023)/1024) vector(1024) independent", //
            "   #pragma acc kernels loop gang((end+127)/128) vector(128)", //
            "   #pragma acc kernels loop gang((end+127)/128) vector(128) ", //
            "   #pragma acc kernels loop gang((end+127)/128) vector(128) independent", //
            "   #pragma acc loop gang reduction(+:gc) ", //
            "   #pragma acc loop gang worker vector reduction(+:temp1,temp2)", //
            "   #pragma acc loop vector", //
            "   #pragma acc loop worker", //
            "   #pragma acc loop worker vector reduction(+:d)", //
            "   #pragma acc parallel loop gang present(rsd) num_gangs(nz-2)", // JO
            "   #pragma acc parallel loop gang present(u,rsd) num_gangs(nz-2)", // JO
            "   #pragma acc parallel loop gang worker vector num_gangs((end+127)/128) num_workers(4)", // JO
            "   #pragma acc parallel loop num_gangs((end+127)/128) num_workers(4)", // JO
            "   #pragma acc parallel num_gangs((end+127)/128) num_workers(4) vector_length(32)", //
            "   #pragma acc parallel num_gangs(end) num_workers(4) vector_length(32)", //
            "   #pragma acc update device(indxp,jndxp,np)", //
            "     #pragma acc loop vector", //
            "    #pragma acc loop vector ", //
            "        #pragma acc loop vector", //
            "      #pragma acc loop vector", //
            "      #pragma acc loop worker", //
            "      #pragma acc loop worker vector", //
            "      #pragma acc update host(rsdnm)", //
            "     #pragma acc loop vector", //
            "     #pragma acc loop vector ", //
            "    #pragma acc  parallel loop gang num_gangs(gp02) num_workers(4) vector_length(32)", //
            "    #pragma acc  parallel loop gang num_gangs(gp02) num_workers(4) vector_length(32) ", //
            "    #pragma acc loop gang vector", //
            "    #pragma acc loop vector", //
            "    #pragma acc loop worker", //
            "    #pragma acc loop worker ", //
            "    #pragma acc loop worker vector", //
            "    #pragma acc parallel loop gang num_gangs(gp02) num_workers(4) vector_length(32)", //
            "    #pragma acc parallel loop gang num_gangs(gp02) num_workers(4) vector_length(32) ", //
            "    #pragma acc parallel loop gang num_gangs(gp02) num_workers(4) vector_length(32)  ", //
            "    #pragma acc parallel loop gang num_gangs(gp02) num_workers(8) vector_length(32)", //
            "    #pragma acc parallel loop gang num_gangs(gp02) num_workers(8) vector_length(32) ", //
            "    #pragma acc parallel loop gang num_gangs(gp12) num_workers(4) vector_length(32)", //
            "    #pragma acc parallel loop gang num_gangs(gp12) num_workers(8) vector_length(32)", //
            "    #pragma acc parallel loop gang num_gangs(gp12) num_workers(8) vector_length(32) ", //
            "    #pragma acc parallel loop gang num_gangs(mm3-1) num_workers(8) vector_length(128)", //
            "    #pragma acc parallel loop gang num_gangs(mm3-d3) num_workers(8) vector_length(128)", //
            "    //#pragma acc kernels loop gang(gp12) ", //
            "    //#pragma acc loop gang(8) vector(32)", //
            "    //#pragma acc loop private(i,j,k)", //
            "   #pragma acc loop vector", //
            "   #pragma acc loop worker", //
            "   #pragma acc loop worker vector", //
            "   #pragma acc parallel loop  gang num_gangs(jend-jst+1) num_workers(num_workers2) vector_length(32)", //
            "   #pragma acc parallel loop  gang num_gangs(jend-jst+1) num_workers(num_workers3) vector_length(32)", //
            "  #pragma acc  parallel loop gang num_gangs(192) num_workers(16) vector_length(32)", //
            "  #pragma acc  parallel loop gang num_gangs(gp12) num_workers(4) vector_length(32) ", //
            "  #pragma acc data deviceptr(r1,r2)", // JO
            "  #pragma acc data deviceptr(u1,u2) ", // JO
            "  #pragma acc data deviceptr(x1,y1)", // JO
            "  #pragma acc data deviceptr(z1,z2,z3)", // JO
            "  #pragma acc data present(ou[0:n3*n2*n1])", //
            "  #pragma acc kernels loop gang((end+127)/128) vector(128) ", //
            "  #pragma acc kernels loop gang((na_gangs+127)/128) vector(128)", //
            "  #pragma acc kernels loop gang((na_gangs+127)/128) vector(128) ", //
            "  #pragma acc kernels loop gang((naa+127)/128) vector(128) independent", //
            "  #pragma acc loop gang", //
            "  #pragma acc loop gang vector", //
            "  #pragma acc loop gang worker vector reduction(+:sx,sy)", //
            "  #pragma acc loop vector", //
            "  #pragma acc loop worker", //
            "  #pragma acc loop worker ", //
            "  #pragma acc loop worker vector", //
            "  #pragma acc parallel loop  gang num_gangs(jend-jst+1) num_workers(num_workers3) vector_length(32)", //
            "  #pragma acc parallel loop gang  num_gangs(nz2) num_workers(8) vector_length(32)", //
            "  #pragma acc parallel loop gang num_gangs(192) num_workers(16) vector_length(32)", //
            "  #pragma acc parallel loop gang num_gangs(192) num_workers(16) vector_length(32) ", //
            "  #pragma acc parallel loop gang num_gangs(end)", // JO
            "  #pragma acc parallel loop gang num_gangs(gp2-2) num_workers(8) vector_length(32)", //
            "  #pragma acc parallel loop gang num_gangs(gp22) num_workers(4) vector_length(32) ", //
            "  #pragma acc parallel loop gang num_gangs(jend-jst+1) num_workers(8) vector_length(32)", //
            "  #pragma acc parallel loop gang num_gangs(jend-jst+1) num_workers(num_workers3) vector_length(32)", //
            "  #pragma acc parallel loop gang num_gangs(m3j-2) num_workers(8) vector_length(128)", //
            "  #pragma acc parallel loop gang num_gangs(n2) vector_length(128)", //
            "  #pragma acc parallel loop gang num_gangs(n3-2) num_workers(16) vector_length(64)", //
            "  #pragma acc parallel loop gang num_gangs(n3-2) vector_length(128)", //
            "  #pragma acc parallel loop gang num_gangs(ny2) num_workers(8) vector_length(32)", //
            "  #pragma acc parallel loop gang num_gangs(nz) num_workers(8) vector_length(128)", //
            "  #pragma acc parallel loop gang num_gangs(nz) num_workers(8) vector_length(32)", //
            "  #pragma acc parallel loop gang num_gangs(nz-2) num_workers(8) vector_length(32)", //
            "  #pragma acc parallel loop gang num_gangs(nz-2) num_workers(num_workers2) vector_length(32)", //
            "  #pragma acc parallel loop gang num_gangs(nz-2) num_workers(num_workers3) vector_length(32)", //
            "  #pragma acc parallel loop gang num_gangs(nz2) num_workers(8) vector_length(32)", //
            "  #pragma acc parallel loop gang vector num_gangs((npl+127)/128)", // JO
            "  #pragma acc parallel loop gang vector num_gangs(1) num_workers(1) vector_length(32)", //
            "  #pragma acc parallel loop gang worker vector", // JO
            "  #pragma acc parallel loop gang worker vector num_gangs((lastcol-firstcol+1+127)/128)", // JO
            "  #pragma acc parallel loop gang worker vector num_gangs((npl+127)/128)", // JO
            "  #pragma acc parallel loop num_gangs(1) num_workers(1) vector_length(32)", //
            "  #pragma acc parallel num_gangs(1) num_workers(1) vector_length(1)", //
            "  #pragma acc parallel num_gangs(NQ) num_workers(4) vector_length(32)", // JO
            "  #pragma acc parallel num_gangs(d2) vector_length(128)", // JO
            "  #pragma acc parallel num_gangs(d3) num_workers(8) vector_length(128)", // JO
            "  #pragma acc parallel num_gangs(d3) vector_length(128)", // JO
            "  #pragma acc update device(forcing)", //
            "  #pragma acc update device(oz[0:n3*n2*n1])", //
            "  #pragma acc update device(u)", //
            "  #pragma acc update host(rhs)", //
            "  #pragma acc update host(u)", //
            "  //#pragma acc kernels present(indxp, jndxp, np)", //
            "  //#pragma acc loop gang vector", //
            "  //#pragma acc loop gang(gp22/128) vector(128)", //
            "  //#pragma acc update device(x[0:2*NK])", //
            "  //#pragma acc update host(oz[0:n3*n2*n1])", //
            "  //#pragma acc update host(p[0:NA+2])", //
            "  //#pragma acc update host(rsd) ", //
            // " #pragma acc data present(colidx[0:nz],", // JO
            " #pragma acc loop gang", //
            " #pragma acc loop vector", //
            " #pragma acc loop worker ", //
            " #pragma acc loop worker vector", //
            " #pragma acc parallel loop gang num_gangs(192) num_workers(16) vector_length(32)", //
            " #pragma acc parallel loop gang num_gangs(ny2) num_workers(8) vector_length(32)", //
            " #pragma acc parallel loop gang num_gangs(nz-2) num_workers(num_workers3) vector_length(32)", //
            " #pragma acc parallel loop gang reduction(+:rsdnm0,rsdnm1,rsdnm2,rsdnm3,rsdnm4)", // JO
            " #pragma acc parallel loop gang reduction(+:s) reduction(max:temp)", // JO
            " #pragma acc parallel num_gangs((ln+127)/128) vector_length(128) present(u_real,u_imag)", //
            " #pragma acc parallel num_gangs(d3) num_workers(8) vector_length(128)", // JO
            " #pragma acc parallel num_gangs(d3) vector_length(128)", // JO
            "#pragma acc data copyin(ce) present(flux_G,frct,rsd)", //
            // "#pragma acc data copyin(colidx[0:nz],a[0:nz],", // JO
            "#pragma acc data create(a,b,c,d,flux_G,indxp,jndxp,np,rho_i,frct,qs,rsd,u,tmat,tv,utmp_G,rtmp_G)", // JO
            "#pragma acc data create(forcing,rho_i,u,us,vs,ws,square,qs,rhs) //fjacX,njacX,lhsX,fjacY,njacY,lhsY,fjacZ,njacZ,lhsZ)", //
            "#pragma acc data create(u,us,vs,ws,qs,rho_i,speed,square,forcing,rhs)", //
            // "#pragma acc data create(u0_real,u0_imag,u1_real,u1_imag,u_real,u_imag,", // JO
            "#pragma acc data create(u[0:gnr],v[0:gnr],r[0:gnr])", //
            "#pragma acc data create(xx[0:blksize*2*NK],qq[0:blksize*NQ]) copyout(q[0:NQ])", //
            "#pragma acc data pcopyin(or[0:n3*n2*n1])", //
            "#pragma acc data present(flux_G,rho_i,frct,qs,rsd,u,utmp_G,rtmp_G)", //
            "#pragma acc data present(forcing,rho_i,u,us,vs,ws,square,qs,rhs) ", //
            "#pragma acc data present(indxp,jndxp,np,a,b,c,d,rho_i,qs,u)", //
            "#pragma acc data present(indxp,jndxp,np,a,b,c,d,rsd,tmat,tv)", //
            "#pragma acc data present(rho_i,u,qs,rhs,square) create(lhsX,fjacX,njacX) ", //
            "#pragma acc data present(rho_i,u,qs,square,speed,rhs,forcing,us,vs,ws) ", //
            "#pragma acc data present(rho_i,u,rhs,square,qs) create(lhsY,fjacY,njacY)", //
            "#pragma acc data present(rho_i,us,speed,rhs) create(lhsX,lhspX,lhsmX,rhonX,rhsX)", //
            "#pragma acc data present(rho_i,vs,speed,rhs) create(lhsY,lhspY,lhsmY,rhoqY)", //
            "#pragma acc data present(rho_i,ws,speed,rhs) create(lhsZ,lhspZ,lhsmZ,rhosZ)", //
            "#pragma acc data present(rsd,rsdnm)", //
            "#pragma acc data present(u,rhs,square,qs) create(lhsZ,fjacZ,njacZ)", //
            "#pragma acc loop vector", //
            "#pragma acc loop worker", //
            "#pragma acc loop worker ", //
            "#pragma acc loop worker vector", //
            "#pragma acc parallel loop gang num_gangs(gp2) num_workers(8) vector_length(32)", //
            "#pragma acc parallel loop gang num_gangs(gp2-2) num_workers(8) vector_length(32)", //
            "#pragma acc parallel loop gang num_gangs(jend-jst+1) num_workers(num_workers3) vector_length(32)", //
            "#pragma acc parallel loop gang num_gangs(mm3-1) num_workers(8) vector_length(128)", //
            "#pragma acc parallel loop gang num_gangs(ny2) num_workers(8) vector_length(32)", //
            "#pragma acc parallel loop gang num_gangs(nz-2) num_workers(num_workers2) vector_length(32)", //
            "#pragma acc parallel loop gang num_gangs(nz-2) num_workers(num_workers3) vector_length(32)", //
            "#pragma acc parallel loop gang num_gangs(nz-2) num_workers(num_workers4) vector_length(128)", //
            "#pragma acc parallel loop gang num_gangs(nz2) num_workers(8) vector_length(32)", //
            "#pragma acc parallel loop gang num_gangs(nz2) num_workers(8) vector_length(32) ", //
            "#pragma acc parallel loop gang num_gangs(nz2+1) num_workers(8) vector_length(32)", //
            "#pragma acc parallel loop gang num_gangs(nz2-4) num_workers(8) vector_length(32)", //
            "#pragma acc parallel loop gang present(u,rhs) num_gangs(gp22) num_workers(4) vector_length(32)", //
            "#pragma acc parallel loop gang worker vector present(a,b,c,d)", // JO
            "#pragma acc parallel num_gangs((NQ+127)/128) vector_length(128) present(q[0:NQ])", //
            "#pragma acc parallel num_gangs((blksize+255)/256) num_workers(1) vector_length(256)", // JO
            "#pragma acc parallel num_gangs(1) num_workers(1)", // JO
            "#pragma acc parallel num_gangs(1) num_workers(1) vector_length(1) present(u_real,u_imag)", //
            "#pragma acc parallel num_gangs(blksize) vector_length(128) present(qq[0:blksize*NQ])", //
            "#pragma acc parallel num_gangs(blksize) vector_length(128) present(xx[0:blksize*2*NK])", //
            "#pragma acc parallel num_gangs(d3) num_workers(8) vector_length(128) present(twiddle)", //
            "#pragma acc parallel num_gangs(n3-2) num_workers(8) vector_length(128)", //
            "#pragma acc parallel num_gangs(n3-2) num_workers(8) vector_length(128) ", //
            "#pragma acc parallel present(oz[0:n3*n2*n1]) num_gangs(n3) num_workers(8) vector_length(128)", //
            "#pragma acc parallel present(rho_i,us,vs,ws,rhs,speed,qs)", // JO
            "#pragma acc parallel present(rhs) num_gangs(nz2) num_workers(8) vector_length(32)", //
            "#pragma acc parallel present(rhs,u) num_gangs(nz2) num_workers(8) vector_length(32)", //
            "#pragma acc parallel present(us,vs,ws,qs,u,speed,rhs) num_gangs(nz2) num_workers(8) vector_length(32)", //
            "#pragma acc update device(u1_real,u1_imag)", //
            "//#pragma acc data present(rho_i,u,qs,rhs,square,lhsX,fjacX,njacX) ", //
            "//#pragma acc data present(rho_i,u,rhs,square,qs,lhsY,fjacY,njacY)", //
            "//#pragma acc data present(u,rhs,square,qs,lhsZ,fjacZ,njacZ)", //
            "//#pragma acc kernels loop present(rhs)", //
            "//#pragma acc kernels loop private(cgit,j,k)", //
            "//#pragma acc parallel num_gangs(d3/128) vector_length(128) present(u1_real,u1_imag) copyin(starts[0:d3])", //
            "//#pragma acc update host(u1_real,u1_imag)", //
    };

    private String[] version20Pragmas = { //
            "#pragma acc parallel default(none)", //
            "#pragma acc kernels default(none)", //
            "#pragma acc parallel loop default(none)", //
            "#pragma acc kernels loop default(none)", //
            "#pragma acc parallel wait(var)", //
            "#pragma acc parallel wait(var1, var2)", //
            "#pragma acc parallel wait(var1, 2)", //
            "#pragma acc wait async(5)", //
            "#pragma acc wait async(var)", //
            "#pragma acc enter data copyin(CG[0:n*n]), present_or_copyin(A[0:n*n], B[0:n*n])", //
            "#pragma acc exit data copyout(CG[0:n*n]), delete(A[0:n*n], B[0:n*n])", //
            "#pragma acc declare link(a)", //
            "#pragma acc loop tile(3, 5)", //
            "#pragma acc parallel loop tile(3, 5)", //
            "#pragma acc kernels loop tile(3, 5)", //
            "#pragma acc loop auto", //
            "#pragma acc parallel loop auto", //
            "#pragma acc kernels loop auto", //
            "#pragma acc update self(a)", //
            "#pragma acc atomic", //
            "#pragma acc atomic read", //
            "#pragma acc atomic write", //
            "#pragma acc atomic update", //
            "#pragma acc atomic capture", //
    };

    @Test
    public void testOpenACCPragmas() throws IOException, SyntaxException, Exception {
        test(epccPragmas);
        test(openaccExamplePragmas);
        test(npbPragmas);
        test(version20Pragmas);
    }

    private void test(String[] pragmas) throws Exception {
        for (String pragma : pragmas) {
            try {
                assertNotNull(new OpenACCParser().parse(pragma));
            } catch (Exception e) {
                System.err.println("Error parsing " + pragma);
                throw e;
            }
        }
    }
}
