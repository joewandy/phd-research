function hdp = init_hdp(input_hdp, debug)

    % copy all the parameters from the input object ..
    
    hdp.J = input_hdp.J;                        % number of replicates
    hdp.NSAMPS = input_hdp.NSAMPS;              % total number of samples
    hdp.BURN_IN = input_hdp.BURN_IN;            % initial burn-in samples

    hdp.mu_0 = input_hdp.mu_0;                  % mean for base distribution of metabolite RT
    hdp.sigma_0_prec = input_hdp.sigma_0_prec;  % prec for base distribution of metabolite RT
    hdp.psi_0 = input_hdp.psi_0;                % mean for base distribution of mass
    hdp.rho_0_prec = input_hdp.rho_0_prec;      % precision for base distribution of mass
    
    hdp.alpha_rt = input_hdp.alpha_rt;          % RT cluster DP concentration param
    hdp.alpha_mass = input_hdp.alpha_mass;      % mass cluster DP concentration param
    hdp.top_alpha = input_hdp.top_alpha;        % metabolite DP concentration param    

    hdp.gamma_prec = input_hdp.gamma_prec;      % precision for RT cluster mixture components
    hdp.rho_prec = input_hdp.rho_prec;          % precision for mass cluster mixture components
    hdp.delta_prec = input_hdp.delta_prec;      % precision for top component Gaussians    
        
    if debug
        hdp.gtI = input_hdp.I;                  % for debugging
    end
    
    % sample initial metabolite RT    
    hdp.I = 1;                                  % initial number of metabolites
    hdp.fi = zeros(1, hdp.I);                   % 1 x I, number of clusters assigned to metabolite i    
    hdp.ti = normrnd(hdp.mu_0, sqrt(1/hdp.sigma_0_prec), 1, hdp.I); % 1 x I, the metabolite's RT

    % assign peaks across files into 1 RT cluster per file, 1 top-level metabolite
    fa = 0;
    sa = 0;
    n_all = [];
    j_all = [];
    data_all = [];
    for j = 1:hdp.J

        hdp.file{j}.K = 1; % number of clusters in this file
        hdp.file{j}.peakID = input_hdp.file{j}.peakID;
        hdp.file{j}.N = length(hdp.file{j}.peakID);
        hdp.file{j}.data_rt = input_hdp.file{j}.data_rt;
        hdp.file{j}.data_mass = input_hdp.file{j}.data_mass;
        hdp.file{j}.data_intensity = input_hdp.file{j}.data_intensity;
        hdp.file{j}.peakID = input_hdp.file{j}.peakID;
        
        if debug
            hdp.file{j}.ground_truth = input_hdp.file{j}.ground_truth; % for debugging
        end        
        
        fa = fa + hdp.file{j}.N;
        sa = sa + sum(hdp.file{j}.data_mass);    
        n_all = [n_all, 1:hdp.file{j}.N];
        j_all = [j_all, repmat([j], 1, hdp.file{j}.N)];
        for n = 1:hdp.file{j}.N
            this_peak.rt = hdp.file{j}.data_rt(n);
            this_peak.mass = hdp.file{j}.data_mass(n);
            this_peak.intensity = hdp.file{j}.data_intensity(n);
            this_peak.peakID = hdp.file{j}.peakID(n);
            if debug
                this_peak.ground_truth = hdp.file{j}.ground_truth(n);
            end
            data_all = [data_all, this_peak];
        end
        
        % K x I, clusters to metabolites assignment
        top_Z = rand(hdp.file{j}.K, hdp.I);
        top_Z = (top_Z==repmat(max(top_Z, [], 2), 1, hdp.I));
        hdp.file{j}.top_Z = top_Z;
        
        % N x K, peaks to clusters assignment
        Z = rand(hdp.file{j}.N, hdp.file{j}.K);             % just randomly assign to start with
        Z = (Z==repmat(max(Z, [], 2), 1, hdp.file{j}.K));   % turn to binary values
        hdp.file{j}.Z = Z;
                                    
        % 1 x K, sample initial the clusters' RT
        for k=hdp.file{j}.K
            parent_i = find(hdp.file{j}.top_Z(k, :)); % find cluster's parent metabolite
            ti = hdp.ti(parent_i);
            tij = normrnd(ti, sqrt(1/hdp.delta_prec)); % then sample cluster rt given ti
            hdp.file{j}.ti(k) = tij;
        end

        % 1 x I, no. of RT clusters under each metabolite
        hdp.fi = hdp.fi + sum(hdp.file{j}.top_Z, 1);    

        % 1 x K, no. of peaks under each RT cluster
        hdp.file{j}.count_Z = sum(hdp.file{j}.Z, 1);
                                                  
    end
    
    % assign all peaks under 1 mass cluster
    for i = 1:hdp.I
    
        hdp.metabolite(i).A = 1;        % number of mass cluster in this metabolite
        hdp.metabolite(i).thetas = [normrnd(hdp.psi_0, sqrt(1/hdp.rho_0_prec))]; % sample some initial value for mass cluster
        hdp.metabolite(i).fa = [fa];    % number of peaks under each mass cluster, size 1 by A
        hdp.metabolite(i).sa = [sa];    % sum of masses of peaks under each mass cluster, size 1 by A
        
        all_peaks_count = length(n_all);
        hdp.metabolite(i).V = repmat([1], 1, all_peaks_count); % assignment of peak n to mass cluster a, size 1 by N
        hdp.metabolite(i).peak_j = j_all; % originating file of peak n, size 1 by N
        hdp.metabolite(i).peak_n = n_all; % index of peak n in file j, size 1 by N
        hdp.metabolite(i).peak_data = data_all; % the peaks themselves
        
    end
    
end
