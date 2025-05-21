(function ($) {
    "use strict";
    /* -------------------------------------------------------------
                        * 프로젝트 함수
    --------------------------------------------------------------*/
    let isEmailAvailable = false;
    let isAuthentication = false;

    // 내실 API 요청
    $.ajax({
        url: '/collectibles',
        type: 'GET',
        success: function(data) {
            collectibleChart(data);
        },
        error: function(xhr, status, error) {
            console.error(xhr.statusText + '\n\n' + xhr.responseText);
        }
    });

    function collectibleChart(collectibleItemList) {
        // collectibleItemList에서 Labels와 데이터 추출 (Type과 비율 계산)
        window.collectibleLabels = collectibleItemList.map(item => item.collectibleTypeName);
        window.collectibleData = collectibleItemList.map(item => (item.totalCollectedTypePoint / item.totalCollectibleTypePoint) * 100);
    
        // 콘솔에 전역 변수 출력
        console.log("Labels:", window.collectibleLabels);
        console.log("Data:", window.collectibleData);
    
        var ctx1 = $("#collectable-percent").get(0).getContext("2d");
        var myChart1 = new Chart(ctx1, {
            type: "bar",
            data: {
                // HTML에서 전달받은 전역 변수로 설정된 데이터를 사용
                labels: window.collectibleLabels || ["No Data"], // 데이터가 없을 경우 기본값 "No Data"
                datasets: [{
                    label: "%",
                    data: window.collectibleData || [0], // 데이터가 없으면 기본값 0
                    backgroundColor: "rgba(235, 22, 22, .7)"
                }]
            },
            options: {
                responsive: true,
                scales: {
                    y: {
                        beginAtZero: true,
                        max: 100 // 100%까지 표시
                    }
                }
            }
        }); 
    }

    //회원가입
    window.checkSignup = function() {
        //유효성 검사
        var form = document.signupForm;
        var emailTest =  /^[0-9a-zA-Z]([-_.]?[0-9a-zA-Z])*@[0-9a-zA-Z]([-_.]?[0-9a-zA-Z])*.[a-zA-Z]{2,3}$/i;
        var passwdTest = /^(?=.*[a-zA-Z])(?=.*[0-9])/;
        if(form.signupId.value=="") {
            alert("이메일을 입력해주세요.");
            form.signupId.focus();
            return false;
        } else if (!emailTest.test(form.signupId.value)) {
            alert("이메일 형식으로 입력해주세요.");
            form.signupId.focus();
            return false;
        } else if (isEmailAvailable === false) {
            alert("이메일 인증을 해주세요.");
            form.checkEmail.focus();
            return false;
        } else if (isAuthentication === false) {
            alert("인증을 완료해주세요.");
            form.checkAuth.focus();
            return false;
        } else if (form.signupPW.value=="") {
            alert("비밀번호를 입력해주세요.");
            form.signupPW.focus();
            return false;
        } else if (!passwdTest.test(form.signupPW.value)) {
            alert("비밀번호는 영문+숫자로 구성하여야 합니다.");
            form.signupPW.focus();
            return false;
        } else if (form.signupPWCheck.value=="") {
            alert("비밀번호를 다시 입력해주세요.");
            form.signupPWCheck.focus();
            return false;
        } else if (form.signupPW.value!=form.signupPWCheck.value) {
            alert("비밀번호가 일치하지 않습니다.");
            form.signupPWCheck.focus();
            return false;
        } else if (form.signupRepresentativeCharacter.value=="") {
            alert("대표 캐릭터 닉네임을 입력해주세요.");
            form.signupRepresentativeCharacter.focus();
            return false;
        } else if (!form.signupAgreement.checked) {
            alert("개인정보 이용에 동의해주세요.");
            form.signupAgreement.focus();
            return false;
        }


        //회원가입 프로세스
        $.ajax({
            url: '/member/signup-process',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({ email: form.signupId.value, PW: form.signupPW.value,
            representativeCharacter: form.signupRepresentativeCharacter.value }),
            success: function(response) {
                if(!response) {
                    alert("대표 캐릭터 닉네임을 입력해주세요.");
                    form.signupRepresentativeCharacter.focus();
                    return false;
                } else {
                    window.location.href = "/"
                }
            }
        });
    }

    //로그인
    window.checkSignin = function() {
        var form = document.signinForm;
        if(form.signinId.value=="") {
            alert("이메일을 입력해주세요.");
            form.signinId.focus();
            return false;
        } else if (form.signinPW.value=="") {
            alert("비밀번호를 입력해주세요.");
            form.signinPW.focus();
            return false;
        }

        $.ajax({
            url: '/member/signin-process',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({ email: form.signinId.value, PW: form.signinPW.value, saveId: form.saveId.checked }),
            success: function(response) {
                if(response) {
                    alert("로그인 되었습니다.");
                    window.location.href = "/";
                } else {
                    alert("이메일 혹은 비밀번호가 틀렸습니다.");
                }
            },
            error : function() {
                alert("이메일 혹은 비밀번호가 틀렸습니다.");
            }
        });
    }

    //비밀번호 변경
    window.changePassword = function() {
        var form = document.changePasswordForm;
        var emailTest =  /^[0-9a-zA-Z]([-_.]?[0-9a-zA-Z])*@[0-9a-zA-Z]([-_.]?[0-9a-zA-Z])*.[a-zA-Z]{2,3}$/i;
        var passwdTest = /^(?=.*[a-zA-Z])(?=.*[0-9])/;
        if(form.authId.value=="") {
            alert("이메일을 입력해주세요.");
            form.authId.focus();
            return false;
        } else if (isEmailAvailable === false) {
            alert("이메일 인증을 해주세요.");
            form.authCode.focus();
            return false;
        } else if (isAuthentication === false) {
            alert("인증을 완료해주세요.");
            form.checkAuth.focus();
            return false;
        } else if (form.changePW.value=="") {
            alert("비밀번호를 입력해주세요.");
            form.checkAuthBtn.focus();
            return false;
        } else if (!passwdTest.test(form.changePW.value)) {
            alert("비밀번호는 영문+숫자로 구성하여야 합니다.");
            form.changePW.focus();
            return false;
        } else if (form.changePWCheck.value=="") {
            alert("비밀번호를 다시 입력해주세요.");
            form.changePWCheck.focus();
            return false;
        } else if (form.changePW.value!=form.changePWCheck.value) {
            alert("비밀번호가 일치하지 않습니다.");
            form.changePWCheck.focus();
            return false;
        }

        $.ajax({
            url: '/member/changePassword-process',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({ email: form.authId.value, PW: form.changePW.value}),
            success: function(response) {
                if(response) {
                    alert("비밀번호가 변경되었습니다.");
                    window.location.href = "/member/signin"
                }
            }
        });
    }

    //비밀번호 변경 이메일 확인 및 인증번호 발송
    window.send_password_reset_email = function() {
        const email = $('#authId').val();
        var emailTest =  /^[0-9a-zA-Z]([-_.]?[0-9a-zA-Z])*@[0-9a-zA-Z]([-_.]?[0-9a-zA-Z])*.[a-zA-Z]{2,3}$/i;
        if (!email) {
            alert("이메일을 입력해주세요.");
            return;
        } else if (!emailTest.test(email)) {
            alert("이메일 형식으로 입력해주세요.");
            form.signinId.focus();
            return;
        }

        // AJAX로 이메일 중복 확인 요청
        $.ajax({
            url: '/member/check-email',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({ email: email }),
            success: function (response) {
                if (response) {
                    alert("이메일 전송");
                    isEmailAvailable = true;
                    $.ajax({
                        url: '/send-email',
                        type: 'POST',
                        contentType: 'application/json',
                        data: JSON.stringify({ email: email }),
                        success: function() {
                            document.getElementById("authInput").style.display = "block";
                        }
                    })
                } else {
                    alert("등록되지 않은 이메일입니다.");
                    isEmailAvailable = false;
                }
            },
            error: function () {
                alert("중복 확인 중 문제가 발생했습니다.");
                isEmailAvailable = false;
            }
        });
    }

    //인증번호 확인
    window.checkAuthCodeChangePassword = function() {
        const authCode = $('#authCode').val();
        $.ajax({
            url: '/member/check-auth',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({ authCode: authCode }),
            success: function(response) {
                if (response === "true") {
                    isAuthentication = true;
                    alert("인증에 성공하였습니다.")
                    document.getElementById('authId').disabled = true;
                    document.getElementById('sendEmailBtn').disabled = true;
                    document.getElementById('authCode').disabled = true;
                    document.getElementById('checkAuthBtn').disabled = true;
                } else if (response === "false") {
                    alert("유효하지 않은 인증코드입니다.");
                } else if (response === "expiration") {
                    alert("인증코드가 만료되었습니다.");
                    window.location.href = "/member/changePassword";
                }
            }
        });
    };

    //회원가입 이메일 중복확인 및 인증번호 발송
    window.sendEmail = function() {
        const email = $('#signupId').val();
        var emailTest =  /^[0-9a-zA-Z]([-_.]?[0-9a-zA-Z])*@[0-9a-zA-Z]([-_.]?[0-9a-zA-Z])*.[a-zA-Z]{2,3}$/i;
        if (!email) {
            alert("이메일을 입력해주세요.");
            return;
        } else if (!emailTest.test(email)) {
            alert("이메일 형식으로 입력해주세요.");
            form.signinId.focus();
            return;
        }

        // AJAX로 이메일 중복 확인 요청
        $.ajax({
            url: '/member/check-email',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({ email: email }),
            success: function (response) {
                if (response) {
                    alert("이미 사용 중인 이메일입니다.");
                    isEmailAvailable = false;
                } else {
                    isEmailAvailable = true;
                    alert("이메일 전송");
                    $.ajax({
                        url: '/send-email',
                        type: 'POST',
                        contentType: 'application/json',
                        data: JSON.stringify({ email: email }),
                        success: function() {
                            document.getElementById("inputAuth").style.display = "block";
                        }
                    })
                }
            },
            error: function () {
                alert("중복 확인 중 문제가 발생했습니다.");
                isEmailAvailable = false;
            }
        });
    }

    //인증번호 확인
    window.checkAuthCode = function() {
        const authCode = $('#checkAuth').val();
        $.ajax({
            url: '/member/check-auth',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({ authCode: authCode }),
            success: function(response) {
                if (response === "true") {
                    isAuthentication = true;
                    alert("인증에 성공하였습니다.")
                    document.getElementById('signupId').disabled = true;
                    document.getElementById('sendEmailBtn').disabled = true;
                    document.getElementById('checkAuth').disabled = true;
                    document.getElementById('checkAuthBtn').disabled = true;
                } else if (response === "false") {
                    alert("유효하지 않은 인증코드입니다.");
                } else if (response === "expiration") {
                    alert("인증코드가 만료되었습니다.");
                    window.location.href = "/member/signup";
                }
            }
        });
    };

    //멤버 수정
    window.changeProfile = function() {
        var passwdTest = /^(?=.*[a-zA-Z])(?=.*[0-9])/;
        const memberId = $('#changeMemberId').val();
        const PW = $('#changePassword').val();
        const PWCheck = $('#changePasswordCheck').val();
        const RCN = $('#changeRepresentativeCharacterNickname').val();changeMemberId

        const ajaxList = [];

        if (PW && PWCheck && PW === PWCheck) {
            if(!passwdTest.test(PW)) {
                alert("비밀번호는 영문+숫자로 구성하여야 합니다.");
                return;
            }

            const changePassword = $.ajax({
                url: '/member/changePassword-process',
                type: 'POST',
                contentType: 'application/json',
                data: JSON.stringify({ email: memberId, PW: PW}),
                success: function(response) {
                    if(response) {
                        alert("비밀번호가 변경되었습니다.");
                    }
                }
            });

            ajaxList.push(changePassword);
        } else if (PW || PWCheck) {
            alert("확인된 비밀번호가 일치하지 않습니다.");
        }
        const changeRCN = $.ajax({
            url: '/member/changeRCN',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({ email: memberId, RCN: RCN}),
            success: function(response) {
                if(response) {
                    alert("대표 캐릭터가 변경되었습니다.");
                }
            }
        });

        ajaxList.push(changeRCN);

        $.when.apply($, ajaxList).always(function() {
            window.location.href = "/member/myPage";
        });
    };


    //알람 띄우기
    function showAlert(message) {
        alert(message);
    }
    /* -------------------------------------------------------------
                    * Themewagon Template functions
    --------------------------------------------------------------*/

    

    // Spinner
    var spinner = function () {
        setTimeout(function () {
            if ($('#spinner').length > 0) {
                $('#spinner').removeClass('show');
            }
        }, 1);
    };
    spinner();
    
    
    // Back to top button
    $(window).scroll(function () {
        if ($(this).scrollTop() > 300) {
            $('.back-to-top').fadeIn('slow');
        } else {
            $('.back-to-top').fadeOut('slow');
        }
    });
    $('.back-to-top').click(function () {
        $('html, body').animate({scrollTop: 0});
        return false;
    });


    // Sidebar Toggler
    $('.sidebar-toggler').click(function () {
        $('.sidebar, .content').toggleClass("open");
        return false;
    });


    // Progress Bar
    $('.pg-bar').waypoint(function () {
        $('.progress .progress-bar').each(function () {
            $(this).css("width", $(this).attr("aria-valuenow") + '%');
        });
    }, {offset: '80%'});


    // Calender
    $('#calender').datetimepicker({
        inline: true,
        format: 'L'
    });


    // Testimonials carousel
    $(".testimonial-carousel").owlCarousel({
        autoplay: true,
        smartSpeed: 1000,
        items: 1,
        dots: true,
        loop: true,
        nav : false
    });


    // Chart Global Color
    Chart.defaults.color = "#6C7293";
    Chart.defaults.borderColor = "#000000";


    // Salse & Revenue Chart
    var ctx2 = $("#salse-revenue").get(0).getContext("2d");
    var myChart2 = new Chart(ctx2, {
        type: "line",
        data: {
            labels: ["2016", "2017", "2018", "2019", "2020", "2021", "2022"],
            datasets: [{
                    label: "Salse",
                    data: [15, 30, 55, 45, 70, 65, 85],
                    backgroundColor: "rgba(235, 22, 22, .7)",
                    fill: true
                },
                {
                    label: "Revenue",
                    data: [99, 135, 170, 130, 190, 180, 270],
                    backgroundColor: "rgba(235, 22, 22, .5)",
                    fill: true
                }
            ]
            },
        options: {
            responsive: true
        }
    });


    // Single Line Chart
    var ctx3 = $("#line-chart").get(0).getContext("2d");
    var myChart3 = new Chart(ctx3, {
        type: "line",
        data: {
            labels: [50, 60, 70, 80, 90, 100, 110, 120, 130, 140, 150],
            datasets: [{
                label: "Salse",
                fill: false,
                backgroundColor: "rgba(235, 22, 22, .7)",
                data: [7, 8, 8, 9, 9, 9, 10, 11, 14, 14, 15]
            }]
        },
        options: {
            responsive: true
        }
    });
    

    // Single Bar Chart
    var ctx4 = $("#bar-chart").get(0).getContext("2d");
    var myChart4 = new Chart(ctx4, {
        type: "bar",
        data: {
            labels: ["Italy", "France", "Spain", "USA", "Argentina"],
            datasets: [{
                backgroundColor: [
                    "rgba(235, 22, 22, .7)",
                    "rgba(235, 22, 22, .6)",
                    "rgba(235, 22, 22, .5)",
                    "rgba(235, 22, 22, .4)",
                    "rgba(235, 22, 22, .3)"
                ],
                data: [55, 49, 44, 24, 15]
            }]
        },
        options: {
            responsive: true
        }
    });


    // Pie Chart
    var ctx5 = $("#pie-chart").get(0).getContext("2d");
    var myChart5 = new Chart(ctx5, {
        type: "pie",
        data: {
            labels: ["Italy", "France", "Spain", "USA", "Argentina"],
            datasets: [{
                backgroundColor: [
                    "rgba(235, 22, 22, .7)",
                    "rgba(235, 22, 22, .6)",
                    "rgba(235, 22, 22, .5)",
                    "rgba(235, 22, 22, .4)",
                    "rgba(235, 22, 22, .3)"
                ],
                data: [55, 49, 44, 24, 15]
            }]
        },
        options: {
            responsive: true
        }
    });


    // Doughnut Chart
    var ctx6 = $("#doughnut-chart").get(0).getContext("2d");
    var myChart6 = new Chart(ctx6, {
        type: "doughnut",
        data: {
            labels: ["Italy", "France", "Spain", "USA", "Argentina"],
            datasets: [{
                backgroundColor: [
                    "rgba(235, 22, 22, .7)",
                    "rgba(235, 22, 22, .6)",
                    "rgba(235, 22, 22, .5)",
                    "rgba(235, 22, 22, .4)",
                    "rgba(235, 22, 22, .3)"
                ],
                data: [55, 49, 44, 24, 15]
            }]
        },
        options: {
            responsive: true
        }
    });
})(jQuery);