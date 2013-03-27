% motor_MC.m
% 
% Given the following variables, this will comput the stall torque [Nm] and
% max rotational speed [rad/s] of a permenant magnet brushless DC electric
% motor.
% 
% This file is built to be used from within ModelCenter to optimize an
% electric motor.
% 
% ModelCenter Variable Definition
% variable: powerMax double output
% variable: omegaMax double output
% variable: D_1in double input
% variable: D_w double input
% variable: l double input
% variable: kD1 double input
% variable: kD2 double input


% %%%%%%%%%%%%%%%%%%%%%%%%%%%% function %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Motor Parameters
m_1 = 3;                        % number of phases
poles = 9;                      % number of poles
p = poles/2;                    
alpha = 0.84;                   % Pole pitch to shoe width ratio
k_f = (4/pi)*sin(alpha*pi/2);   % exitation field form factor
mu_not = 0.4*pi*10^-6;          % mag perm of free space
mu_rrec = 1.05;                 % relative recoil permiability
B_r = 1.45;                     % remenance magnetic field (Neodymium Iron Boron)(T)
k_w = 0.926;                    % winding factor
k_p = pi*sqrt(3)/6;             % packing factor
k_g = 0.98;                     % gap coefficient--changed from 0.95, gap was too large CJP
rho_w = 1.68*10^(-8);           % resistivity of copper (ohm*meter)
density_steel = 7850;           % density of steel (kg/m^3)
density_copper = 8940;          % density of copper (kg/m^3)
density_magnet = 7500;          % density of Neodymium Iron Boron (kg/m^3)
k_c = 1.1;    % confirm value!  % Carters coefficient k_c > 1
k_sat = 1.1;  % confirm value!  % saturation factor k_sat > 1
Rexternal = 1;                  % resistance of all the external components --CJP

% Geometry
% D_1in = 0.132;                % stator interior diameter
r_1in = D_1in/2;
D_1out = kD1*D_1in;             % stator exterior diameter
r_1out = D_1out/2;
D_2out =  k_g*D_1in;            % rotor exterior diameter
r_2out = D_2out/2;
D_2in = kD2*D_2out;              % rotor interior diameter
r_2in = D_2in/2;
g = r_1in - r_2out;             % air gap distance -- changed CJP
r_w = D_w/2;

% Functions

% Motor Constants
tau = pi*D_1in/(2*p); % pole pitch
b_p = alpha*tau;    % pole shoe width
h_a =  r_1out - r_1in; % armature pole length
N_p = floor(0.225*tau*k_p*(r_1out-r_1in)/(pi*r_w^2)); % number of windings per armature pole
N = 2*p*N_p/m_1; % number of turns per phase 
% N = 192;   % number of turns per phase
h_m = r_2out - r_2in;  % magnet thickness
B_mg = B_r/(1 + mu_rrec*(g/h_m)); 
% B_mg = 0.923; % max B in air gap
B_mg1 = k_f*B_mg; % B_mg at fundamental harmonic
phi_f = (2/pi)*tau*l*B_mg1; % excitation flux
phi_f_sq = b_p*l*B_mg;
kE = 8*p*N_p*k_w*phi_f_sq/(2*pi);  %using N_p% EMF constant (V-s)
% kE = 8*p*N*k_w*phi_f_sq/(2*pi);  % EMF constant (V-s)
kT = kE;  % torque constant (Nm/A)

% Resistance
l_w = N_p*(2*(l + 0.3*tau) + 2*0.7*tau)*3*3; % times 9 since there are thre poles per phase and 3 phases
A_w = pi*r_w^2; 

% assume that the total resistance is the resistance of the
% external components + the resistance of the wire -- CJP
Resistance_w = rho_w*l_w/A_w;
Rtotal = Rexternal + Resistance_w;

% Inductance
% armature inductance
g_prime = k_c*k_sat*g + (h_m/mu_rrec);
L_a = mu_not*(pi/12)*(D_1in/g_prime)*(alpha^3)*l*...
    (N/(4*p*3))^2;

% Mass
Ao = 0.225*tau*k_p*(r_1out-r_1in); % cross sectional area taken up by one side of a winding
v1 = (pi*(r_1out*r_1out - r_1in*r_1in) - 4*Ao*(poles+1))*l;  % stator volume (4 should be 2, but numbers are too big)
v2 = pi*(r_2out*r_2out - r_2in*r_2in)*l; % rotor volume
vol_wire = A_w*l_w; %volume of windings
mass_stator = v1*density_steel;
mass_rotor = v2*density_magnet;
mass_wire = vol_wire*density_copper;
mass = mass_stator + mass_rotor + mass_wire;

%Moment of inertia
Inertia = 0.5*mass*(r_2out*r_2out + r_2in*r_2in);

% T = kT*I_rms_sq;    % torque (Nm)
% E = kE*omega;      % EMF  (V)

V = 500;
b = 0.1;
I = V/Rtotal;
P = I*V;
if N_p == 0
    tauStall = 0;
    omegaMax = 0;
else
    tauStall = kT*V/Rtotal;
    omegaMax = V*kT/(Rtotal*b+kT*kT);%*(30/pi);
end

powerMax = tauStall*omegaMax/4;


