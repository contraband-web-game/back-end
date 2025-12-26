(function () {
    const RULE_IMAGES = [
        '/images/1. 소개.png',
        '/images/2. 라운드 진행.png',
        '/images/3. 결과.png',
        '/images/4. 승패.png'
    ];

    function ensureStyles() {
        if (document.getElementById('rules-modal-style')) return;
        const style = document.createElement('style');
        style.id = 'rules-modal-style';
        style.textContent = `
        .rules-floating-btn {
            position: fixed;
            left: 18px;
            bottom: 18px;
            z-index: 120;
            background: #2f1b1b;
            color: #d5bdaf;
            border: 2px solid #000;
            border-radius: 14px 14px 6px 6px;
            padding: 10px 14px;
            font-family: 'Amatic SC','East Sea Dokdo',cursive;
            font-size: 1.5rem;
            box-shadow: 3px 3px 0 rgba(0,0,0,0.5);
            cursor: pointer;
            transition: transform 0.1s;
        }
        .rules-floating-btn:hover { transform: translateY(-2px) scale(1.02); color: #fff; }
        .rules-modal-backdrop {
            position: fixed;
            inset: 0;
            background: rgba(0,0,0,0.85);
            display: none;
            align-items: center;
            justify-content: center;
            z-index: 130;
        }
        .rules-modal-backdrop.show { display: flex; }
        .rules-modal {
            background: #d5bdaf;
            border: 3px solid #000;
            box-shadow: 8px 8px 0 rgba(0,0,0,0.6);
            border-radius: 6px;
            max-width: 900px;
            width: 90%;
            max-height: 90vh;
            overflow: hidden;
            position: relative;
        }
        .rules-modal header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 10px 14px;
            background: #2f1b1b;
            color: #d5bdaf;
            font-family: 'Creepster','Amatic SC','East Sea Dokdo',cursive;
            letter-spacing: 1px;
        }
        .rules-modal button.rules-close {
            background: transparent;
            color: inherit;
            border: none;
            font-size: 1.5rem;
            cursor: pointer;
        }
        .rules-modal .rules-body {
            padding: 12px 14px 16px;
            display: flex;
            flex-direction: column;
            gap: 10px;
        }
        .rules-modal img {
            width: 100%;
            max-height: 70vh;
            object-fit: contain;
            border: 2px solid #000;
            background: #fff;
        }
        .rules-modal .rules-controls {
            display: flex;
            justify-content: space-between;
            align-items: center;
            font-family: 'Amatic SC','East Sea Dokdo',cursive;
            font-size: 1.4rem;
            color: #2f1b1b;
        }
        .rules-modal .rules-nav {
            display: flex;
            gap: 8px;
        }
        .rules-modal .rules-nav button {
            background: #2f1b1b;
            color: #d5bdaf;
            border: 2px solid #000;
            border-radius: 6px;
            padding: 6px 12px;
            font-size: 1rem;
            cursor: pointer;
            box-shadow: 2px 2px 0 rgba(0,0,0,0.4);
        }
        .rules-modal .rules-nav button:disabled {
            opacity: 0.5;
            cursor: not-allowed;
            box-shadow: none;
        }
        `;
        document.head.appendChild(style);
    }

    function createModal() {
        const backdrop = document.createElement('div');
        backdrop.className = 'rules-modal-backdrop';
        const modal = document.createElement('div');
        modal.className = 'rules-modal';

        modal.innerHTML = `
            <header>
                <span>CONTRABAND 작전 규칙</span>
                <button class="rules-close" aria-label="닫기">✕</button>
            </header>
            <div class="rules-body">
                <img id="rules-image" alt="게임 규칙">
                <div class="rules-controls">
                    <div class="rules-nav">
                        <button id="rules-prev">이전</button>
                        <button id="rules-next">다음</button>
                    </div>
                    <span id="rules-counter"></span>
                </div>
            </div>
        `;
        backdrop.appendChild(modal);
        document.body.appendChild(backdrop);
        return { backdrop, modal };
    }

    function initRules() {
        if (window.location.pathname.endsWith('/index.html')) return;
        if (!document.body) return;
        ensureStyles();
        const { backdrop, modal } = createModal();
        const imgEl = modal.querySelector('#rules-image');
        const counterEl = modal.querySelector('#rules-counter');
        const btnPrev = modal.querySelector('#rules-prev');
        const btnNext = modal.querySelector('#rules-next');
        const closeBtn = modal.querySelector('.rules-close');
        let idx = 0;

        function update() {
            imgEl.src = RULE_IMAGES[idx] || '';
            counterEl.textContent = `${idx + 1} / ${RULE_IMAGES.length}`;
            btnPrev.disabled = idx === 0;
            btnNext.disabled = idx === RULE_IMAGES.length - 1;
        }

        function open() {
            idx = 0;
            update();
            backdrop.classList.add('show');
        }

        function close() {
            backdrop.classList.remove('show');
        }

        btnPrev.addEventListener('click', () => {
            if (idx > 0) {
                idx -= 1;
                update();
            }
        });
        btnNext.addEventListener('click', () => {
            if (idx < RULE_IMAGES.length - 1) {
                idx += 1;
                update();
            }
        });
        closeBtn.addEventListener('click', close);
        backdrop.addEventListener('click', (e) => {
            if (e.target === backdrop) close();
        });

        const btn = document.createElement('button');
        btn.className = 'rules-floating-btn';
        btn.type = 'button';
        btn.textContent = '게임 룰';
        btn.addEventListener('click', open);
        document.body.appendChild(btn);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initRules);
    } else {
        initRules();
    }
})();
