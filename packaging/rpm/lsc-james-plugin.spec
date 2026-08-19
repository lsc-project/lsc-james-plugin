%global lsc_min_version		2.3

%bcond_with build_from_sources

Name:          lsc-james-plugin
Version:       1.2
Release:       1%{?dist}
Summary:       LSC James plugin
License:       BSD-3-Clause
URL:           https://lsc-project.org
%if %{with build_from_sources}
Source0:       https://github.com/lsc-project/%{name}/archive/v%{version}/%{name}-%{version}.tar.gz
%else
Source1:       https://www.lsc-project.org/archives/%{name}-%{version}-distribution.jar
%endif
BuildArch:     noarch

BuildRequires: coreutils
%if %{with build_from_sources}
BuildRequires: jpackage-utils
%if 0%{?fedora} || 0%{?rhel} >=10
BuildRequires: java-devel >= 1:21
BuildRequires: maven
BuildRequires: maven-local
%else
%if 0%{?el9}
BuildRequires: java-21-devel
BuildRequires: maven-openjdk21
BuildRequires: maven-local-openjdk21
%endif
%endif
%endif
Requires:      lsc >= %{lsc_min_version}


%description
This is a James plugin for LSC.


%prep
%if %{with build_from_sources}
%setup -q
%endif


%build
%if %{with build_from_sources}
mvn package
%endif


%install
# Jar
mkdir -p %{buildroot}%{_libdir}/lsc
%if %{with build_from_sources}
install -m 0644 target/%{name}-%{version}-distribution.jar \
  %{buildroot}%{_libdir}/lsc
%else
install -m 0644 %{SOURCE1} %{buildroot}%{_libdir}/lsc/
%endif


%files
%if %{with build_from_sources}
%license LICENSE.txt
%doc README.md
%doc samples/
%endif
%{_libdir}/lsc/%{name}-%{version}-distribution.jar


%changelog
* Wed Aug 19 2026 Xavier Bachelot <xavier.bachelot@worteks.com> - 1.2-1
- Initial specfile
